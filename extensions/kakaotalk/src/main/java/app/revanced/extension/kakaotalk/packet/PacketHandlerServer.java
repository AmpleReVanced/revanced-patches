package app.revanced.extension.kakaotalk.packet;

import android.util.Log;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class PacketHandlerServer {

    private static final String TAG = "PacketHandlerServer";
    private static final int BACKLOG_SIZE = 128;
    
    private final int port;
    private static final ChannelGroup connectedChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static PacketRequestHandler requestHandler;

    public PacketHandlerServer(int port) {
        this.port = port;
    }

    public static void setRequestHandler(PacketRequestHandler handler) {
        requestHandler = handler;
    }

    public static void sendToAll(byte[] data) {
        for (Channel channel : connectedChannels) {
            if (channel.isActive()) {
                channel.writeAndFlush(Unpooled.copiedBuffer(data));
            }
        }
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new LineFrameDecoder())
                                    .addLast(new PacketHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, BACKLOG_SIZE)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind("0.0.0.0", port).sync();
            Log.i(TAG, "Server started on port " + port);

            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private static class LineFrameDecoder extends ChannelInboundHandlerAdapter {
        
        private static final int MAX_FRAME_LENGTH = 1048576;
        private static final byte LINE_FEED = '\n';
        
        private ByteBuf buffer;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf in = (ByteBuf) msg;
            
            try {
                if (buffer == null) {
                    buffer = ctx.alloc().buffer();
                }
                
                buffer.writeBytes(in);
                
                while (buffer.isReadable()) {
                    int lineEndIndex = findLineEnd(buffer);
                    
                    if (lineEndIndex == -1) {
                        break;
                    }
                    
                    int frameLength = lineEndIndex - buffer.readerIndex();
                    
                    if (frameLength > MAX_FRAME_LENGTH) {
                        buffer.skipBytes(frameLength + 1);
                        Log.w(TAG, "Frame too large, skipped");
                        continue;
                    }
                    
                    ByteBuf frame = buffer.readRetainedSlice(frameLength);
                    buffer.skipBytes(1);
                    
                    ctx.fireChannelRead(frame);
                }
                
                buffer.discardReadBytes();
            } finally {
                in.release();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (buffer != null && buffer.refCnt() > 0) {
                buffer.release();
                buffer = null;
            }
            super.channelInactive(ctx);
        }

        private int findLineEnd(ByteBuf buf) {
            int totalLength = buf.readableBytes();
            return buf.indexOf(buf.readerIndex(), buf.readerIndex() + totalLength, LINE_FEED);
        }
    }

    private static class PacketHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            connectedChannels.add(ctx.channel());
            Log.i(TAG, "Client connected: " + ctx.channel().remoteAddress() + " (total: " + connectedChannels.size() + ")");
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Log.i(TAG, "Client disconnected: " + ctx.channel().remoteAddress() + " (total: " + connectedChannels.size() + ")");
            super.channelInactive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf buffer = (ByteBuf) msg;
            
            try {
                String jsonString = buffer.toString(StandardCharsets.UTF_8);
                JSONObject request = new JSONObject(jsonString);

                if (requestHandler != null) {
                    JSONObject response = requestHandler.handleRequest(request);
                    sendResponse(ctx, response);
                } else {
                    Log.w(TAG, "No request handler set");
                }
            } catch (JSONException e) {
                Log.e(TAG, "Invalid JSON received", e);
                sendErrorResponse(ctx, "Invalid JSON format");
            } finally {
                buffer.release();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            Log.e(TAG, "Exception in handler", cause);
            ctx.close();
        }

        private void sendResponse(ChannelHandlerContext ctx, JSONObject response) {
            if (response != null) {
                String responseString = response.toString() + "\n";
                ctx.writeAndFlush(Unpooled.copiedBuffer(responseString, StandardCharsets.UTF_8));
            }
        }

        private void sendErrorResponse(ChannelHandlerContext ctx, String message) {
            try {
                JSONObject error = new JSONObject();
                error.put("status", "error");
                error.put("message", message);
                sendResponse(ctx, error);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to create error response", e);
            }
        }
    }

    public interface PacketRequestHandler {
        JSONObject handleRequest(JSONObject packet);
    }
}
