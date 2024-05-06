import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorWEB{
	public static final int PUERTO = 1234;
    private ServerSocket ss;
    private ExecutorService pool;
		
		class Manejador extends Thread{
			protected Socket socket;
			protected PrintWriter pw;
			protected BufferedOutputStream bos;
			protected BufferedReader br;
			protected String FileName;
			
			public Manejador(Socket _socket) throws Exception{
				this.socket=_socket;
			}
			
			public void run(){
				try{
					br=new BufferedReader(new InputStreamReader(socket.getInputStream()));
					bos=new BufferedOutputStream(socket.getOutputStream());
					pw=new PrintWriter(new OutputStreamWriter(bos));
					String line=br.readLine();
					System.out.println(line);
					if(line==null){
						pw.print("<html><head><title>Servidor WEB");
						pw.print("</title><body bgcolor=\"#AACCFF\"<br>Linea Vacia</br>");
						pw.print("</body></html>");
						socket.close();
						return;
					}
					System.out.println("\nCliente Conectado desde: "+socket.getInetAddress());
					System.out.println("Por el puerto: "+socket.getPort());
					System.out.println("Datos: "+line+"\r\n\r\n");
					
					if(line.toUpperCase().startsWith("GET")){
                        if (line.indexOf("?") == -1) {
                            getArch(line);
                            if(FileName != null && FileName.compareTo("")==0){
                                SendA("index.htm");
                            }
                            else{
                                SendA(FileName != null ? FileName : "index.htm");
                            }
                            System.out.println(FileName != null ? FileName : "");
                        }
                        else{
                            /*Tokenizacion de la linea hasta encontrar ? */
                            StringTokenizer tokens=new StringTokenizer(line,"?");
                            String req_a=tokens.nextToken();
                            String req=tokens.nextToken();
                            System.out.println("Token1: "+req_a+"\r\n\r\n");
                            System.out.println("Token2: "+req+"\r\n\r\n");
                            pw.println("HTTP/1.0 200 Okay");
                            pw.flush();
                            pw.println();
                            pw.flush();
                            pw.print("<html><head><title>SERVIDOR WEB");
                            pw.flush();
                            pw.print("</title></head><body bgcolor=\"#AACCFF\"><center><h1><br>Parametros Obtenidos..</br></h1>");
                            pw.flush();
                            pw.print("<h3><b>"+req+"</b></h3>");
                            pw.flush();
                            pw.print("</center></body></html>");
                            pw.flush();
                        }
					} else if (line.toUpperCase().startsWith("POST")) {
                        handlePostRequest(line);
                    } else if (line.toUpperCase().startsWith("PUT")){
                        handlePutRequest(line);
                    } else if(line.toUpperCase().startsWith("HEAD")) {
                        handleHeadRequest(line);
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
					bos.flush();
				}
				catch(Exception e){
					e.printStackTrace();
				}
				try{
					socket.close();
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}

            
			
			public void getArch(String line){
				int i;
				int f;
				if(line.toUpperCase().startsWith("GET")){
					i=line.indexOf("/");
					f=line.indexOf(" ",i);/*Hasta el final de la linea, desde indexOf "/" */
					FileName=line.substring(i+1,f);/*Encuentra la subcadena en el rango establecido */
				}
			}

			public void SendA(String fileName,Socket sc){
				//System.out.println(fileName);
				int fSize = 0;
				byte[] buffer = new byte[4096];
				try{
					DataOutputStream out =new DataOutputStream(sc.getOutputStream());
					
					//sendHeader();
					FileInputStream f = new FileInputStream(fileName);
					int x = 0;
					while((x = f.read(buffer))>0){
				//		System.out.println(x);
						out.write(buffer,0,x);
					}
					out.flush();
					f.close();
				}catch(FileNotFoundException e){
					//msg.printErr("Transaction::sendResponse():1", "El archivo no existe: " + fileName);
				}catch(IOException e){
		//			System.out.println(e.getMessage());
					//msg.printErr("Transaction::sendResponse():2", "Error en la lectura del archivo: " + fileName);
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
                // Mapear las extensiones de archivo a los tipos de contenido MIME correspondientes
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
                // Por defecto, devolver "application/octet-stream" para tipos de contenido desconocidos
                return "application/octet-stream";
            }
            

            private void handlePostRequest(String line) throws IOException {
                // Leer la solicitud POST para obtener los datos del formulario
                StringBuilder requestBody = new StringBuilder();
                int contentLength = 0;
                while ((line = br.readLine()) != null && line.length() != 0) {
                    /*Verifica que un parte del cuerpo contenga content-length */
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                    }
                }
                /*Si hay  */
                if (contentLength > 0) {
                    char[] buffer = new char[contentLength];
                    br.read(buffer, 0, contentLength);
                    requestBody.append(buffer);
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
        /*Mapea los parametros del body name=***&password=*****
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

            private void handlePutRequest(String line) throws IOException {
                String[] parts = line.split(" ");
                String fileName = parts[1].substring(1);
            
                File file = new File(fileName);
                boolean fileExists = file.exists();

                Date date = new Date();
            
                try (FileWriter writer = new FileWriter(file, false)) { // El segundo parámetro es 'false' para sobrescribir el contenido existente
                    // Leer el cuerpo de la solicitud y escribirlo en el archivo
                    String requestBody;
                    if (fileExists) {
                        writer.write("Contenido actualizado");
                        writer.write("\n");
                    }
                    writer.write(date.toString() + "\n\n");
                    while ((requestBody = br.readLine()) != null && requestBody.length() != 0) {
                        writer.write(requestBody);
                        writer.write("\n"); // Agregar un salto de línea después de cada línea
                    }
                    writer.flush();
                }
            
                String response;
                if (fileExists) {
                    response = "HTTP/1.1 200 OK\n" +
                            "Content-Type: text/html\n" +
                            "\n" +
                            "<html><body><h1>Archivo actualizado exitosamente!</h1></body></html>";
                } else {
                    response = "HTTP/1.1 201 Created\n" +
                            "Content-Type: text/html\n" +
                            "\n" +
                            "<html><body><h1>Archivo creado exitosamente!</h1></body></html>";
                }
                pw.write(response);
                pw.flush();
            }
            
            private void handleHeadRequest(String line) throws IOException {
                String[] parts = line.split(" ");
                String fileName = parts[1].substring(1);
            
                File file = new File(fileName);
                boolean fileExists = file.exists();
            
                String response;
                if (fileExists) {
                    response = "HTTP/1.1 200 OK\n" +
                               "Content-Type: text/html\n" +
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