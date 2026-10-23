typedef unsigned char uint8_t;
typedef unsigned int uint32_t;
typedef signed int int32_t;
typedef unsigned long uint64_t;
typedef signed long int64_t;
typedef unsigned long size_t;

#define READ_BUFFER_SIZE 4096
#define MAP_LINE_SIZE 8192
#define AT_FDCWD -100
#define SYS_OPENAT 56
#define SYS_CLOSE 57
#define SYS_READ 63
#define SYS_MPROTECT 226
#define EINTR 4
#define PROT_READ 1
#define PROT_WRITE 2
#define PROT_EXEC 4

static char read_buffer[READ_BUFFER_SIZE];
static char map_line[MAP_LINE_SIZE];
static int patch_count;
static int patch_error;

static int contains_path(const char *start, const char *end, const char *needle) {
    for (; start < end; start++) {
        const char *left = start;
        const char *right = needle;
        while (*right && left < end && *left == *right) {
            left++;
            right++;
        }
        if (!*right && (left == end || *left == ' ' || *left == '!' || *left == '/')) return 1;
    }
    return 0;
}

static long system_call(long number, long argument0, long argument1, long argument2) {
    register long x8 __asm__("x8") = number;
    register long x0 __asm__("x0") = argument0;
    register long x1 __asm__("x1") = argument1;
    register long x2 __asm__("x2") = argument2;
    __asm__ volatile(
        "svc #0"
        : "+r"(x0)
        : "r"(x8), "r"(x1), "r"(x2)
        : "memory", "cc"
    );
    return x0;
}

static uint64_t parse_hex(const char **position, const char *end) {
    uint64_t value = 0;
    while (*position < end) {
        char current = **position;
        uint32_t digit;
        if (current >= '0' && current <= '9') digit = current - '0';
        else if (current >= 'a' && current <= 'f') digit = current - 'a' + 10;
        else break;
        value = value * 16 + digit;
        (*position)++;
    }
    return value;
}

static int matches(const uint32_t *code) {
    return code[0] == 0x321c03e2 &&
        code[1] == 0xaa1503e0 &&
        code[2] == 0xaa1403e1 &&
        (code[3] & 0xfc000000) == 0x94000000 &&
        (code[4] & 0xff00001f) == 0x34000000 &&
        code[5] == 0x91004294 &&
        code[6] == 0x110006f7 &&
        code[7] == 0x6b1302ff &&
        (code[8] & 0xff00001f) == 0x54000003;
}

static void patch(uint32_t *code, uint64_t start, uint64_t finish, int protection) {
    int needs_write = !(protection & PROT_WRITE);
    if (needs_write && system_call(SYS_MPROTECT, start, finish - start, protection | PROT_WRITE) < 0) {
        patch_error = -2;
        return;
    }

    uint32_t immediate = (code[4] >> 5) & 0x7ffff;
    int64_t signed_immediate = ((int64_t)(int32_t)(immediate << 13)) >> 13;
    code[4] = 0x14000000 | ((uint32_t)signed_immediate & 0x03ffffff);
    __asm__ volatile(
        "dc cvau, %0\n"
        "dsb ish\n"
        "ic ivau, %0\n"
        "dsb ish\n"
        "isb\n"
        :
        : "r"(&code[4])
        : "memory"
    );
    patch_count++;

    // Use the mapping boundaries so this also works with 16 KB memory pages.
    if (needs_write && system_call(SYS_MPROTECT, start, finish - start, protection) < 0) {
        patch_error = -3;
    }
}

static void scan_mapping(const char *position, const char *end) {
    const char *cursor = position;
    uint64_t start = parse_hex(&cursor, end);
    if (cursor == position || cursor == end || *cursor++ != '-') return;
    const char *finish_start = cursor;
    uint64_t finish = parse_hex(&cursor, end);
    if (cursor == finish_start || cursor == end || *cursor != ' ' || finish <= start) return;
    while (cursor < end && *cursor == ' ') cursor++;
    if (end - cursor < 4 || cursor[0] != 'r' || cursor[2] != 'x' || cursor[3] != 'p') return;
    if (cursor[1] != 'w' && cursor[1] != '-') return;
    if (!contains_path(cursor, end, ".apk") && !contains_path(cursor, end, "/libbaiduprotect.so")) return;
    if (finish - start < 9 * sizeof(uint32_t)) return;

    int protection = PROT_READ | PROT_EXEC | (cursor[1] == 'w' ? PROT_WRITE : 0);
    uint32_t *code = (uint32_t *)((start + 3) & ~3UL);
    uint32_t *code_end = (uint32_t *)(finish - 9 * sizeof(uint32_t));
    while (code <= code_end) {
        // More than one executable mapping can contain the integrity check.
        if (matches(code)) patch(code, start, finish, protection);
        code++;
    }
}

__attribute__((constructor)) static void initialize(void) {
    static const char path[] = "/proc/self/maps";
    long descriptor;
    do {
        descriptor = system_call(SYS_OPENAT, AT_FDCWD, (long)path, 0);
    } while (descriptor == -EINTR);
    if (descriptor < 0) {
        patch_error = -1;
        return;
    }

    size_t size = 0;
    int truncated = 0;
    for (;;) {
        long count = system_call(SYS_READ, descriptor, (long)read_buffer, sizeof(read_buffer));
        if (count == -EINTR) continue;
        if (count < 0) patch_error = -1;
        if (count <= 0) break;
        for (long index = 0; index < count; index++) {
            char current = read_buffer[index];
            if (current == '\n') {
                if (!truncated) scan_mapping(map_line, map_line + size);
                size = 0;
                truncated = 0;
            } else if (size < sizeof(map_line)) {
                map_line[size++] = current;
            } else {
                truncated = 1;
            }
        }
    }
    if (!truncated && size) scan_mapping(map_line, map_line + size);
    system_call(SYS_CLOSE, descriptor, 0, 0);
}

// Positive: patched checks; zero: no match; negative: maps I/O or protection failure.
__attribute__((visibility("default")))
int Java_com_sagittarius_v6_StubApplication_getStarnoteIntegrityPatchStatus(void *environment, void *application) {
    (void)environment;
    (void)application;
    return patch_error ? patch_error : patch_count;
}