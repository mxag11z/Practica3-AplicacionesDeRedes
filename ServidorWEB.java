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
                        // No hay parametros en la solicitud GET
                        getArch(peticion);// extraer el nombre del archivo
                        if (FileName != null && FileName.compareTo("") == 0) {
                            //si FileName no es nulo y no está vacío, se envía ese archivo. 
                            //De lo contrario, se envía el archivo "index.htm".
                            SendA("index.htm");
                        } else {
                            SendA(FileName != null ? FileName : "index.htm");
                        }
                        System.out.println(FileName != null ? FileName : "");
                    } else {
                        /* Tokenizacion de la linea hasta encontrar ? */
                        /* Si la solicitud contiene un signo de interrogación ('?'), 
                        significa que hay parámetros en la solicitud. tokeniza la solicitud 
                        para obtener la parte de la URL antes del signo de interrogación (req_a) 
                        y los parámetros después del signo de interrogación (req)*/
                        StringTokenizer tokens = new StringTokenizer(peticion, "?");
                        String req_a = tokens.nextToken();
                        String req = tokens.nextToken();
                        System.out.println("Token1: " + req_a + "\r\n\r\n");
                        System.out.println("Token2: " + req + "\r\n\r\n");
                        /* Generar respuesta HTTP para el cliente */
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

            if (FileName.isEmpty() || FileName.equals("/")) {
                // Establecer 'index.htm' como el archivo por defecto si el nombre de archivo está vacío o es solo '/'
                FileName = "index.htm";
            } else {
                // Verificar si el archivo existe
                File file = new File(FileName);
                if (!file.exists() || file.isDirectory()) {
                    // El archivo no existe o es un directorio
                    FileName = "error404.html"; // Establecer un valor especial para FileName
                }
            }
        }

        public void SendA(String fileName) {
            try {
                if (fileName.equals("error404.html")) {
                    // El archivo solicitado no existe, enviar página de error 404
                    String error404Content = "<html><head><title>Error 404 - Not Found</title></head>" +
                            "<body><h1>Error 404 - Not Found</h1>" +
                            "<p>The requested URL was not found on this server.</p>" +
                            "</body></html>";
                    String response = "HTTP/1.1 404 Not Found\n" +
                            "Content-Type: text/html\n" +
                            "Content-Length: " + error404Content.length() + "\n\n" +
                            error404Content;
                    pw.write(response);
                    pw.flush();
                } else {
                    int b_leidos = 0;
                    // para leer el contenido del archivo
                    BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(fileName));
                    byte[] buf = new byte[1024];
                    // obtenemos el tamaño del archivo
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
                    /* mientras se puedan leer bytes del BufferedInputStream (bis2). 
                    bis2.read(buf, 0, buf.length) lee bytes del archivo y los almacena 
                    en el búfer de bytes buf. 
                    La función devuelve el número de bytes leídos, que se almacena en b_leidos. 
                    El bucle se ejecutará hasta que bis2.read() devuelva -1,
                    lo que indica que no hay más datos que leer. */
                    while ((b_leidos = bis2.read(buf, 0, buf.length)) != -1) {
                        bos.write(buf, 0, b_leidos);
                    }
                    bos.flush();
                    bis2.close();
                }
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
            mimeTypes.put("pdf", "application/pdf");

            // Obtener la extensión del archivo
            int dotIndex = fileName.lastIndexOf('.'); // busca el ultimo punto en el fileName
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
            StringBuilder requestBody = new StringBuilder(); //  almacenar el cuerpo de la solicitud POST
            int contentLength = 0;
        
            // Leer el cuerpo del mensaje
            char[] buffer = new char[4096];
            try (BufferedReader br = new BufferedReader(new StringReader(line))) {
                /*  lee la cabecera de la solicitud para encontrar la longitud del 
                contenido (Content-Length). Si se encuentra, se almacena en contentLength. */
                while ((line = br.readLine()) != null && line.length() != 0) {
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim()); //.trim() elimina espacios en blanco
                    }
                }
                /* si contentLength es mayor que 0, se lee el contenido del cuerpo de la solicitud 
                en un búfer y se agrega al StringBuilder requestBody */
                if (contentLength > 0) {
                    br.read(buffer, 0, contentLength);
                    requestBody.append(buffer);
                }
            }
        
            // Parsear los datos del formulario
            String postData = requestBody.toString(); // El cuerpo de la solicitud se convierte en una cadena (postData)
            Map<String, String> formData = parseFormData(postData);
        
            System.out.println("Datos del formulario recibidos:");
            /* iteramos sobre el mapa formData y se imprimen las claves y 
            valores de los datos del formulario en la consola. */
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

        private Map<String, String> parseFormData(String postData) {
            Map<String, String> formData = new HashMap<>(); // para almacenar los pares clave-valor extraídos de los datos del formulario.
            String[] params = postData.split("&"); // Se divide la cadena postData en un arreglo de cadenas params, usando el carácter '&' como delimitador.
            for (String param : params) {
                /* itera sobre cada parámetro en el arreglo params. Para cada parámetro, se divide en una clave y un valor usando el carácter '=' como delimitador.  */
                String[] keyValue = param.split("=");
                /* Si el resultado de la división tiene exactamente dos partes (una clave y un valor), se agrega al Map formData. */
                if (keyValue.length == 2) {
                    formData.put(keyValue[0], keyValue[1]);
                }
            }
            return formData;
        }

        /* línea de solicitud (line), la longitud total de los datos leídos (t), y un array de bytes (b) que contiene los datos de la solicitud.  */
        private void handlePutRequest(String line, int t, byte[] b) throws IOException {
            System.out.println("*********LINE ENTRANDO AL PUT*********8\n" + line + "\n");
            /* divide la línea de solicitud en partes separadas por espacios y se extrae el nombre del archivo de la segunda parte de la línea, eliminando el primer carácter (/). */
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

                    /* Se extrae la subcadena que contiene el encabezado Content-Length completo, desde startIndex hasta endIndex */
                    contentLengthLine = line.substring(startIndex, endIndex);
                    System.out.println(contentLengthLine);
                    /* Se divide contentLengthLine en dos partes utilizando ":" como delimitador.  */
                    String[] partes = contentLengthLine.split(":");
                    System.out.println(partes[0] + "\n");
                    System.out.println(partes[1] + "\n");
                    if (partes.length == 2) {

                        /* se convierte la segunda parte (el valor del contenido) en un entero. trim() se usa para eliminar espacios en blanco  */
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

            int headersEndIndex = line.indexOf("\r\n\r\n");
            if (headersEndIndex == -1) {
                System.err.println("Error: No se encontró el final de los encabezados.");
                return;
            }

            // Calcula el inicio del cuerpo (después del final de los encabezados)
            /* Calcula el índice donde comienza el cuerpo de la solicitud. Sumando 4 al índice del final de los encabezados (headersEndIndex), se salta el espacio adicional que indica el final de los encabezados antes del cuerpo. */
            int bodyStartIndex = headersEndIndex + 4;
            /* Calcula el desplazamiento necesario para encontrar el inicio del cuerpo en el array de bytes b */
            int offset = t - contentLength;
            System.out.println(offset);
            // Extrae el cuerpo de la solicitud
            String bodywrite = line.substring(bodyStartIndex);

            System.out.println("Body: " + bodywrite);

            /* Convierte el cuerpo de la solicitud en un array de bytes */
            byte[] bodyBytes = bodywrite.getBytes(StandardCharsets.UTF_8);

            /* Escribe los datos del cuerpo de la solicitud en un archivo en el servidor utilizando un FileOutputStream. */
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(b, offset, t);
                fos.close();
                System.out.println("Cuerpo escrito en el archivo.");
            } catch (IOException e) {
                System.err.println("Error al escribir en el archivo: " + e.getMessage());
            }
            try (PrintWriter pw = new PrintWriter(socket.getOutputStream())) {
                /* respuesta para el cliente */
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
        this.pool = Executors.newFixedThreadPool(2); 
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