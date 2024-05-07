import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorWEB {
    public static final int PUERTO = 1234;
    private ServerSocket ss;
    private ExecutorService pool;

    class Manejador extends Thread {
        protected Socket socket;
        protected PrintWriter pw;
        protected BufferedOutputStream bos;
        protected BufferedReader br;
        protected String FileName;
        protected DataInputStream dis;
        protected DataOutputStream dos;

        public Manejador(Socket _socket) throws Exception {
            this.socket = _socket;
        }

        public void run() {
            try {
                // br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                bos = new BufferedOutputStream(socket.getOutputStream());
                pw = new PrintWriter(new OutputStreamWriter(bos));
                /********************* */
                dos = new DataOutputStream(socket.getOutputStream());
                dis = new DataInputStream(socket.getInputStream());
                /******************** */
                byte[] b = new byte[500000];
                int t = dis.read(b);
                String line = new String(b, 0, t);
                System.out.println(line);
                /*Tokenizacion */
                StringTokenizer tokensFirstLine = new StringTokenizer(line, "\n");
                String peticion = tokensFirstLine.nextToken();
                String requestnext = tokensFirstLine.nextToken();
                System.out.println("Esta es la peticion: "+peticion);
                if (line == null) {
                    pw.print("<html><head><title>Servidor WEB");
                    pw.print("</title><body bgcolor=\"#AACCFF\"<br>Linea Vacia</br>");
                    pw.print("</body></html>");
                    socket.close();
                    return;
                }

                System.out.println("\nCliente Conectado desde: " + socket.getInetAddress());
                System.out.println("Por el puerto: " + socket.getPort());
                System.out.println("Datos: " + peticion + "\r\n\r\n");

                if (peticion.toUpperCase().startsWith("GET")) {
                    if (peticion.indexOf("?") == -1) {
                        getArch(peticion);
                        if (FileName != null && FileName.compareTo("") == 0) {
                            SendA("index.htm");
                        } else {
                            SendA(FileName != null ? FileName : "index.htm");
                        }
                        System.out.println(FileName != null ? FileName : "");
                    } else {
                        /* Tokenizacion de la linea hasta encontrar ? */
                        StringTokenizer tokens = new StringTokenizer(peticion, "?");
                        String req_a = tokens.nextToken();
                        String req = tokens.nextToken();
                        System.out.println("Token1: " + req_a + "\r\n\r\n");
                        System.out.println("Token2: " + req + "\r\n\r\n");
                        pw.println("HTTP/1.0 200 Okay");
                        pw.flush();
                        pw.println();
                        pw.flush();
                        pw.print("<html><head><title>SERVIDOR WEB");
                        pw.flush();
                        pw.print(
                                "</title></head><body bgcolor=\"#AACCFF\"><center><h1><br>Parametros Obtenidos..</br></h1>");
                        pw.flush();
                        pw.print("<h3><b>" + req + "</b></h3>");
                        pw.flush();
                        pw.print("</center></body></html>");
                        pw.flush();
                    }
                } else if (line.toUpperCase().startsWith("POST")) {
                    handlePostRequest(line);
                } else if (line.toUpperCase().startsWith("PUT")) {
                    handlePutRequest(line, t, b);
                } else if (line.toUpperCase().startsWith("HEAD")) {
                    handleHeadRequest(peticion);
                } else {
                    String error501 = "HTTP/1.1 501 Not Implemented\n" +
                            "Date: " + new Date() + " \n" +
                            "Server: Axel/1.0 \n" +
                            "Content-Type: text/html \n\n" +

                            "<html><head><meta charset='UTF-8'><title>Error 501</title></head>" +
                            "<body><h1>Error 501: No implementado.</h1>" +
                            "<p>El método HTTP o funcionalidad solicitada no está implementada en el servidor.</p>" +
                            "</body></html>";

                    pw.write(error501);
                    pw.flush();
                    System.out.println("Respuesta ERROR 501: \n" + error501);
                }
                pw.flush();
                // bos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void getArch(String line) {
            int i;
            int f;
            if (line.toUpperCase().startsWith("GET") || line.toUpperCase().startsWith("PUT")) {
                i = line.indexOf("/");
                f = line.indexOf(" ", i);/* Hasta el final de la linea, desde indexOf "/" */
                FileName = line.substring(i + 1, f);/* Encuentra la subcadena en el rango establecido */
            }
        }

       

        public void SendA(String fileName) {
            try {
                int b_leidos = 0;
                BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(fileName));
                byte[] buf = new byte[1024];
                int tam_archivo = bis2.available();

                // Establecer el tipo de contenido basado en la extensión del archivo
                String contentType = getContentType(fileName);

                // Construir la respuesta HTTP con el tipo de contenido apropiado
                String response = "HTTP/1.0 200 OK\n" +
                        "Server: Max Server/1.0\n" +
                        "Date: " + new Date() + "\n" +
                        "Content-Type: " + contentType + "\n" +
                        "Content-Length: " + tam_archivo + "\n\n";

                // Enviar la cabecera de la respuesta
                bos.write(response.getBytes());
                bos.flush();

                // Enviar el contenido del archivo
                while ((b_leidos = bis2.read(buf, 0, buf.length)) != -1) {
                    bos.write(buf, 0, b_leidos);
                }
                bos.flush();
                bis2.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        private String getContentType(String fileName) {
            // Mapear las extensiones de archivo a los tipos de contenido MIME
            // correspondientes
            Map<String, String> mimeTypes = new HashMap<>();
            mimeTypes.put("html", "text/html");
            mimeTypes.put("htm", "text/html");
            mimeTypes.put("txt", "text/plain");
            mimeTypes.put("jpg", "image/jpeg");
            mimeTypes.put("jpeg", "image/jpeg");
            mimeTypes.put("gif", "image/gif");
            mimeTypes.put("png", "image/png");

            // Obtener la extensión del archivo
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex >= 0) {
                String extension = fileName.substring(dotIndex + 1).toLowerCase();
                // Buscar el tipo de contenido MIME correspondiente
                String contentType = mimeTypes.get(extension);
                if (contentType != null) {
                    return contentType;
                }
            }
            // Por defecto, devolver "application/octet-stream" para tipos de contenido
            // desconocidos
            return "application/octet-stream";
        }
        private void handlePostRequest(String line) throws IOException {
            // Leer la solicitud POST para obtener los datos del formulario
            StringBuilder requestBody = new StringBuilder();
            int contentLength = 0;
        
            
        
            // Leer el cuerpo del mensaje
            char[] buffer = new char[4096];
            try (BufferedReader br = new BufferedReader(new StringReader(line))) {
                while ((line = br.readLine()) != null && line.length() != 0) {
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                    }
                }
                if (contentLength > 0) {
                    br.read(buffer, 0, contentLength);
                    requestBody.append(buffer);
                }
            }
        
            // Parsear los datos del formulario
            String postData = requestBody.toString();
            Map<String, String> formData = parseFormData(postData);
        
            // Procesar los datos del formulario como desees
            // Aquí puedes imprimirlos en la consola o realizar alguna otra acción
            System.out.println("Datos del formulario recibidos:");
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        
            // Enviar una respuesta al cliente
            String response = "HTTP/1.1 200 OK\n" +
                    "Content-Type: text/html\n" +
                    "\n" +
                    "<html><body><h1>¡Formulario recibido!</h1></body></html>";
            pw.write(response);
            pw.flush();
        }
        

        // private void handlePostRequest(String line) throws IOException {
        //     // Leer la solicitud POST para obtener los datos del formulario
        //     StringBuilder requestBody = new StringBuilder();
        //     int contentLength = 0;
        //     char[] buffer = new char[4096];
        //     while ((line = br.readLine()) != null && line.length() != 0) {
        //         /* Verifica que un parte del cuerpo contenga content-length */
        //         if (line.startsWith("Content-Length:")) {
        //             contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
        //         }
        //     }

        //     /* Si hay */
        //     if (contentLength > 0) {
        //         br.read(buffer, 0, contentLength);
        //         requestBody.append(buffer);
        //     }

        //     // Parsear los datos del formulario
        //     String postData = requestBody.toString();
        //     Map<String, String> formData = parseFormData(postData);

        //     // Procesar los datos del formulario como desees
        //     // Aquí puedes imprimirlos en la consola o realizar alguna otra acción
        //     System.out.println("Datos del formulario recibidos:");
        //     for (Map.Entry<String, String> entry : formData.entrySet()) {
        //         System.out.println(entry.getKey() + ": " + entry.getValue());
        //     }

        //     // Enviar una respuesta al cliente
        //     String response = "HTTP/1.1 200 OK\n" +
        //             "Content-Type: text/html\n" +
        //             "\n" +
        //             "<html><body><h1>¡Formulario recibido!</h1></body></html>";
        //     pw.write(response);
        //     pw.flush();
        // }

        /*
         * Mapea los parametros del body name=***&password=*****
         * => name(key): ****(value),
         * 
         */
        private Map<String, String> parseFormData(String postData) {
            Map<String, String> formData = new HashMap<>();
            String[] params = postData.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    formData.put(keyValue[0], keyValue[1]);
                }
            }
            return formData;
        }

        private void handlePutRequest(String line, int t, byte[] b) throws IOException {
            System.out.println("*********LINE ENTRANDO AL PUT*********8\n" + line + "\n");
            String[] parts = line.split(" ");
            String fileName = parts[1].substring(1);
            System.out.println("Nombre del archivo: " + fileName);
            File file = new File(fileName);
            boolean fileExists = file.exists();
            System.out.println("Existencia del archivo: " + fileExists);
            // int contentLength = 0;
            String contentType = getContentType(fileName);
            String contentLengthLine = null;
            int contentLength = 0;

            int startIndex = line.indexOf("Content-Length:");
            if (startIndex != -1) {

                int endIndex = line.indexOf("\r\n", startIndex);
                if (endIndex != -1) {

                    contentLengthLine = line.substring(startIndex, endIndex);
                    System.out.println(contentLengthLine);
                    String[] partes = contentLengthLine.split(":");
                    System.out.println(partes[0] + "\n");
                    System.out.println(partes[1] + "\n");
                    if (partes.length == 2) {

                        contentLength = Integer.parseInt(partes[1].trim());
                    }
                }
            }

            System.out.println("Content-length: " + contentLength);

            // Crear el archivo si no existe
            if (!fileExists) {
                try {
                    if (!file.createNewFile()) {
                        throw new IOException("No se creo el archivo: " + fileName);
                    }
                } catch (IOException e) {
                    System.err.println("Error al crear el archivo: " + e.getMessage());
                    return;
                }
            }

            // System.out.println(contentLength);
            // System.out.println(line.length() +"-" +contentLength+ "= "+offset);
            // byte[] buffer = new byte[4096];
            // String body = new String(buffer,offset,contentLength);
            // System.out.println(body);

            int headersEndIndex = line.indexOf("\r\n\r\n");
            if (headersEndIndex == -1) {
                System.err.println("Error: No se encontró el final de los encabezados.");
                return;
            }

            // Calcula el inicio del cuerpo (después del final de los encabezados)
            int bodyStartIndex = headersEndIndex + 4;
            int offset = t - contentLength;
            System.out.println(offset);
            // Extrae el cuerpo de la solicitud
            String bodywrite = line.substring(bodyStartIndex);

            System.out.println("Body: " + bodywrite);

            
            byte[] bodyBytes = bodywrite.getBytes(StandardCharsets.UTF_8);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(b, offset, t);
                fos.close();
                System.out.println("Cuerpo escrito en el archivo.");
            } catch (IOException e) {
                System.err.println("Error al escribir en el archivo: " + e.getMessage());
            }
            try (PrintWriter pw = new PrintWriter(socket.getOutputStream())) {
                String response = fileExists ? "HTTP/1.1 200 OK\n" : "HTTP/1.1 201 Created\n";
                response += "Content-Type: text/html\n\n";
                response += "<html><body><h1>Archivo " + (fileExists ? "actualizado" : "creado") + " exitosamente!</h1></body></html>";
                pw.write(response);
                pw.flush();
            } catch (IOException e) {
                System.err.println("Error al enviar respuesta al cliente: " + e.getMessage());
                // Manejar el error según sea necesario
            }
        }
            // String responseStatus = fileExists ? "200 OK" : "201 Created";
            // String responseBody = "<html><body><h1>Archivo actualizado/creado exitosamente!</h1></body></html>";
            
            // String response = "HTTP/1.1 " + responseStatus + "\r\n" +
            //                   "Content-Type: " + contentType + "\r\n" +
            //                   "Content-Length: " + responseBody.length() + "\r\n" +
            //                   "\r\n" +
            //                   responseBody;
            
            
            // bos.write(response.getBytes());
            // bos.flush();
            
            // System.out.println(response);
        

        private void handleHeadRequest(String line) throws IOException {
            String[] parts = line.split(" ");
            String fileName = parts[1].substring(1);
            String contentType = getContentType(fileName);
            File file = new File(fileName);
            boolean fileExists = file.exists();

            String response;
            if (fileExists) {
                response = "HTTP/1.1 200 OK\n" +
                        "Content-Type:"+contentType+"\n" +
                        "Content-Length: " + file.length() + "\n" + // Longitud del contenido
                        "\n"; // Fin de los encabezados, no hay cuerpo de mensaje en una solicitud HEAD
            } else {
                response = "HTTP/1.1 404 Not Found\n" +
                        "Content-Type: text/html\n" +
                        "\n"; // Fin de los encabezados, no hay cuerpo de mensaje en una solicitud HEAD
            }
            pw.write(response);
            pw.flush();
            System.out.println("Respuesta HEAD: \n" + response);
        }

    }

    public ServidorWEB() throws Exception {
        System.out.println("Iniciando Servidor.......");
        this.ss = new ServerSocket(PUERTO);
        this.pool = Executors.newFixedThreadPool(2); // Cambia el número según tus necesidades
        System.out.println("Servidor iniciado:---OK");
        System.out.println("Esperando por Cliente....");
        while (true) {
            Socket accept = ss.accept();
            pool.execute(new Manejador(accept));
        }
    }

    public static void main(String[] args) throws Exception {
        ServidorWEB sWEB = new ServidorWEB();
    }

}