package Jp;

/* LocoHeader */
public class b {

    /* packetId */
    public final int a;

    /* status */
    public final short b;

    /* method */
    public final c c;

    /* bodyLength */
    public final int d;

    public b(int packetId, short status, c method, int bodyLength) {
        this.a = packetId;
        this.b = status;
        this.c = method;
        this.d = bodyLength;
    }

    /* getBodyLength */
    public final int a() {
        throw new UnsupportedOperationException("Stub!");
    }

    /* getMethod */
    public final c b() {
        throw new UnsupportedOperationException("Stub!");
    }

    /* getPacketId */
    public final int c() {
        throw new UnsupportedOperationException("Stub!");
    }

    /* getStatus */
    public final short d() {
        throw new UnsupportedOperationException("Stub!");
    }

    public boolean equals(Object other) {
        throw new UnsupportedOperationException("Stub!");
    }

    public int hashCode() {
        throw new UnsupportedOperationException("Stub!");
    }

    public String toString() {
        throw new UnsupportedOperationException("Stub!");
    }

}
