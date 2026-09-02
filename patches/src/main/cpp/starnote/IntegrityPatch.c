typedef unsigned char uint8_t;
typedef unsigned int uint32_t;
typedef signed int int32_t;
typedef unsigned long uint64_t;
typedef signed long int64_t;
typedef unsigned long size_t;

#define MAP_BUFFER_SIZE 524288
#define AT_FDCWD -100
#define SYS_OPENAT 56
#define SYS_CLOSE 57
#define SYS_READ 63

static char map_data[MAP_BUFFER_SIZE];

static int contains(const char *start, const char *end, const char *needle) {
    for (; start < end; start++) {
        const char *left = start;
        const char *right = needle;
        while (*right && left < end && *left == *right) {
            left++;
            right++;
        }
        if (!*right) return 1;
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

static void patch(uint32_t *code) {
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
}

static void scan_maps(const char *data, size_t size) {
    const char *position = data;
    const char *end = data + size;
    uint32_t *match = 0;
    size_t match_count = 0;
    while (position < end) {
        const char *line_end = position;
        while (line_end < end && *line_end != '\n') line_end++;
        const char *cursor = position;
        uint64_t start = parse_hex(&cursor, line_end);
        if (cursor < line_end && *cursor == '-') cursor++;
        uint64_t finish = parse_hex(&cursor, line_end);
        while (cursor < line_end && *cursor == ' ') cursor++;
        int executable = cursor + 2 < line_end && cursor[0] == 'r' && cursor[2] == 'x';
        if (executable && contains(cursor, line_end, "/base.apk")) {
            uint32_t *code = (uint32_t *)((start + 3) & ~3UL);
            uint32_t *code_end = (uint32_t *)(finish - 9 * sizeof(uint32_t));
            while (code <= code_end) {
                if (matches(code)) {
                    match = code;
                    match_count++;
                }
                code++;
            }
        }
        position = line_end < end ? line_end + 1 : end;
    }
    if (match_count == 1) patch(match);
}

__attribute__((constructor)) static void initialize(void) {
    static const char path[] = "/proc/self/maps";
    long descriptor = system_call(SYS_OPENAT, AT_FDCWD, (long)path, 0);
    if (descriptor < 0) return;
    size_t size = 0;
    while (size < sizeof(map_data)) {
        long count = system_call(SYS_READ, descriptor, (long)&map_data[size], sizeof(map_data) - size);
        if (count <= 0) break;
        size += count;
    }
    system_call(SYS_CLOSE, descriptor, 0, 0);
    scan_maps(map_data, size);
}