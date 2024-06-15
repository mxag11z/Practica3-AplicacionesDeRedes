import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class SWEBNOBloqueante {
    public static final int PUERTO = 9876;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private ExecutorService threadPool;

    public static void main(String[] args) {
        new SWEBNOBloqueante().start();
    }

    public void start() {
        threadPool = Executors.newFixedThreadPool(10); // Crear una piscina de hilos con 10 hilos.
        
        try {
            // Abrir un canal de servidor y un selector
            serverSocketChannel = ServerSocketChannel.open();// abrimos un canal de servidor de socket
            serverSocketChannel.bind(new InetSocketAddress(PUERTO)); // asociamos el canal de servidor a la ip y puerto.
            serverSocketChannel.configureBlocking(false); // configuramos el canal para que sea no bloqueante
            selector = Selector.open(); //creamos un selector que pueda monitorear múltiples canales para eventos e/s

            // Registra el canal del servidor con el selector para aceptar conexiones.
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true) {
                selector.select(); // bloquea la ejecución hasta que al menos uno de los canales registrados esté listo para una operación de e/s.

                // Iterar sobre las llaves con canales listos
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove(); // elimina la llave actual del para no procesarla despues.

                    if (key.isAcceptable()) {
                        // verifica si la llave está lista para aceptar una nueva conexión.
                        handleAccept(key);

                    } else if (key.isReadable()) {
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ); // deshabilita temporalmente la operación de lectura (OP_READ) en el selector para evitar que se notifique múltiples veces mientras se está procesando la lectura.

                        // Enviar la tarea de lectura a la piscina de hilos.
                        threadPool.submit(() -> {
                            try {
                                handleRead(key); // operación de lectura sobre el canal asociado a key.
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                // verifica si el canal (key.channel()) sigue abierto. Si es así, se vuelve a habilitar la operación de lectura (OP_READ) para notificar cuando el canal esté listo para leer más datos.

                                if (key.channel().isOpen()) {
                                    key.interestOps(key.interestOps() | SelectionKey.OP_READ); // Vuelve a habilitar el OP_READ.
                                }
                            }
                        });
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (threadPool != null) {
                threadPool.shutdown();
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel(); // devuelve el canal asociado con la SelectionKey
        SocketChannel socketChannel = serverChannel.accept(); //  espera y acepta una nueva conexión entrante, devolviendo un SocketChannel que representa esta nueva conexión
        socketChannel.configureBlocking(false); // configura el SocketChannel en modo no bloqueante. 
        socketChannel.register(selector, SelectionKey.OP_READ); //  registra el SocketChannel con el Selector para monitorear operaciones de e/s
    }

    private void handleRead(SelectionKey key) throws IOException {
        // leemos la peticion
        SocketChannel socketChannel = (SocketChannel) key.channel(); // obtenemos el canal asociado con la SelectionKey actual
        ByteBuffer buffer = ByteBuffer.allocate(1024); // reserva un espacio de 1024 bytes para el buffer de lectura.
        int bytesRead = -1;
        
        try {
            bytesRead = socketChannel.read(buffer); // intenta leer datos desde el SocketChannel hacia el ByteBuffer
        } catch (ClosedChannelException e) {
            // El canal ya está cerrado
            return;
        }

        if (bytesRead == -1) {
            // significa que el cliente ha cerrado la conexión y cerramos el canal para liberar recursos
            socketChannel.close();
        } else {
            buffer.flip(); // prepara el ByteBuffer para la lectura al ajustar la posición y límite.
            String request = StandardCharsets.UTF_8.decode(buffer).toString(); // decodifica los datos del buffer usando UTF-8 y los convierte en una cadena
            System.out.println("Request: " + request);

            if (request.startsWith("GET")) {
                handleGetRequest(request, socketChannel);
            } else {
                sendNotImplementedResponse(socketChannel);
            }
        }
    }

    private void handleGetRequest(String request, SocketChannel socketChannel) throws IOException {
        String fileName = getFileNameFromRequest(request); // extraemos el nombre del archivo de la peticion
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
        String contentType = getContentType(filePath.toString()); // obtenemos la extension del archivo 
        byte[] fileContent = Files.readAllBytes(filePath); // lee todo el contenido del archivo especificado por filePath y lo almacena en un arreglo de bytes 

        String responseHeader = "HTTP/1.0 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + fileContent.length + "\r\n\r\n";

        ByteBuffer headerBuffer = ByteBuffer.wrap(responseHeader.getBytes(StandardCharsets.UTF_8)); // convierte el encabezado de respuesta en un ByteBuffer utilizando UTF-8 
        socketChannel.write(headerBuffer); // envía el encabezado como bytes al cliente a través del SocketChannel.

        ByteBuffer contentBuffer = ByteBuffer.wrap(fileContent); // convierte el arreglo de bytes fileContent que contiene el contenido del archivo en un ByteBuffer
        while (contentBuffer.hasRemaining()) {
            // escribe los bytes del contenido del archivo al SocketChannel mientras haya bytes restantes en el ByteBuffer.
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
