# Control de Parqueadero 🚗🏍️

Esta aplicación móvil permite gestionar eficientemente la entrada y salida de vehículos en un parqueadero. La app registra la hora de entrada y salida de cada vehículo, identifica si es un carro o una moto, y almacena la información de la placa mediante reconocimiento automatizado. Además, ofrece una funcionalidad para hacer seguimiento en tiempo real de los puestos ocupados y libres en el parqueadero.

---

## Funcionalidades principales

- **Registro de entrada y salida de vehículos**: Control automático de horarios y clasificación del tipo de vehículo (carro o moto).
- **Reconocimiento de placas**: Utiliza **OpenCV** y el **API de Google Vision** para leer automáticamente las placas de los vehículos.
- **Seguimiento de puestos**: Visualiza en tiempo real los espacios ocupados y disponibles en el parqueadero.
- **Historial**: Consulta registros de entrada y salida con información detallada.

---

## Tecnologías utilizadas

- **Lenguaje**: Java  
- **Entorno de desarrollo**: Android Studio  
- **Procesamiento de imágenes**: OpenCV  
- **Reconocimiento de texto**: API de Google Vision

  ## Maqueta del parqueadero (uso de Arduino y ESP32)
![Captura de la interfaz principal](https://raw.githubusercontent.com/AndreaCTS/imagenes/main/WhatsApp%20Image%202025-01-08%20at%2011.55.00%20AM.jpeg)
  

---

## Capturas de pantalla

En esta parte se muestra en la aplicación el uso de la camara para tomar una foto de la placa, abajo de esta vista se encuestra la imágen procesada para pasar a la api de google y que esta haga el reconocimiento de placa. Finalmente el texto que aprece en la parte final es la placa reconocida
![Captura de la interfaz principal](https://raw.githubusercontent.com/AndreaCTS/imagenes/main/WhatsApp%20Image%202025-01-08%20at%2011.46.31%20AM.jpeg)

En esta sección, parqueadero tiene 8 puestos y entonces al ocupar un carro alguno de los puestos (se controla con hardware) , en la aplicación aparece un carro ocupando la posición del puesto ocupado
![Captura de la interfaz principal](https://raw.githubusercontent.com/AndreaCTS/imagenes/main/WhatsApp%20Image%202025-01-08%20at%2011.47.04%20AM.jpeg)


---

## Cómo instalar

1. Con Android Studio , puedes descargar la apk en tu telefono
