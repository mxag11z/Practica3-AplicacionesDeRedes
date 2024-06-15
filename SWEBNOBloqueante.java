import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public class SWEBNOBloqueante {
    public static final int PUERTO = 1234;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    public static void main(String[] args) {
        new SWEBNOBloqueante().start();
    }

    public void start() {
        try {
            // Abrir un canal de servidor y un selector
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(PUERTO));
            serverSocketChannel.configureBlocking(false);
            selector = Selector.open();

            // Registrar el canal del servidor en el selector
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                // Esperar a que haya algo listo en el selector
                selector.select();

                // Iterar sobre las llaves con canales listos
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = socketChannel.read(buffer);

        if (bytesRead == -1) {
            socketChannel.close();
        } else {
            buffer.flip();
            String request = StandardCharsets.UTF_8.decode(buffer).toString();
            System.out.println("Request: " + request);

            if (request.startsWith("GET")) {
                handleGetRequest(request, socketChannel);
            } else {
                sendNotImplementedResponse(socketChannel);
            }
        }
    }

    private void handleGetRequest(String request, SocketChannel socketChannel) throws IOException {
        String fileName = getFileNameFromRequest(request);
        if (fileName == null || fileName.isEmpty()) {
            fileName = "index.htm";
        }

        Path filePath = Paths.get(fileName);
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendErrorResponse(socketChannel, 404, "Not Found", "The requested URL was not found on this server.");
        } else {
            sendFileResponse(socketChannel, filePath);
        }
    }

    private String getFileNameFromRequest(String request) {
        int startIndex = request.indexOf("/") + 1;
        int endIndex = request.indexOf(" ", startIndex);
        return request.substring(startIndex, endIndex);
    }

    private void sendFileResponse(SocketChannel socketChannel, Path filePath) throws IOException {
        String contentType = getContentType(filePath.toString());
        byte[] fileContent = Files.readAllBytes(filePath);

        String responseHeader = "HTTP/1.0 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + fileContent.length + "\r\n\r\n";

        ByteBuffer headerBuffer = ByteBuffer.wrap(responseHeader.getBytes(StandardCharsets.UTF_8));
        socketChannel.write(headerBuffer);

        ByteBuffer contentBuffer = ByteBuffer.wrap(fileContent);
        while (contentBuffer.hasRemaining()) {
            socketChannel.write(contentBuffer);
        }
    }

    private void sendErrorResponse(SocketChannel socketChannel, int statusCode, String statusMessage, String body) throws IOException {
        String response = "HTTP/1.0 " + statusCode + " " + statusMessage + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + body.length() + "\r\n\r\n" +
                body;

        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
        socketChannel.write(buffer);
    }

    private void sendNotImplementedResponse(SocketChannel socketChannel) throws IOException {
        String body = "<html><head><title>Not Implemented</title></head>" +
                "<body><h1>501 Not Implemented</h1>" +
                "<p>The requested method is not implemented on this server.</p></body></html>";

        sendErrorResponse(socketChannel, 501, "Not Implemented", body);
    }

    private String getContentType(String fileName) {
        Map<String, String> mimeTypes = new HashMap<>();
        mimeTypes.put("html", "text/html");
        mimeTypes.put("htm", "text/html");
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("pdf", "application/pdf");

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            String extension = fileName.substring(dotIndex + 1).toLowerCase();
            String contentType = mimeTypes.get(extension);
            if (contentType != null) {
                return contentType;
            }
        }
        return "application/octet-stream";
    }
}
