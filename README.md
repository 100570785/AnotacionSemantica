# Laboratorio de Anotación Semántica

Este proyecto es una herramienta en Java desarrollada para automatizar la extracción de información y la anotación semántica de páginas web. Utiliza herramientas de Procesamiento de Lenguaje Natural (PLN) e Inteligencia Artificial generativa para extraer entidades nombradas y vincularlas a una base de conocimiento estructurada (Wikipedia) generando un archivo RDF en formato Turtle (`.ttl`).

## ⚙️ Arquitectura y Tecnologías

El sistema se compone de los siguientes elementos principales:
* **Java & Maven:** Lenguaje principal y gestión del ciclo de vida del proyecto.
* **GATE / ANNIE:** Herramienta de extracción de información utilizada para reconocer entidades nombradas (NER) en el texto de las páginas web y clasificarlas en tres tipos: `Person`, `Organization` y `Location`.
* **API de Google Gemini (gemini-2.5-flash):** Modelo de Lenguaje Extenso (LLM) utilizado para desambiguar las entidades de tipo `Location`. Se le proporciona el contexto del documento para que determine la URL exacta de la versión en inglés de Wikipedia que mejor representa ese lugar.
* **RDF Schema (RDFS):** Vocabulario estructurado utilizado para generar el archivo de salida, definiendo las clases (`urn:uc3m.es:miaa#webPage`, `dcterms:Location`) y las propiedades (`miaa:mentionsEntity`, `miaa:mentionsInstance`).

## 📋 Requisitos Previos

1. Tener instalado **Java JDK** (versión 17 o superior recomendada).
2. Tener instalado y configurado **Apache Maven** en las variables de entorno (`MAVEN_HOME` y `Path`).
3. Disponer de una **API Key de Google Gemini**. Debes insertarla manualmente en el archivo `src/main/java/org/example/Annotator.java` en la variable `API_KEY`.

## 🛠️ Instalación de Dependencias Locales

Este proyecto utiliza una librería personalizada (`utils-1.0-jar-with-dependencies.jar`) proporcionada para la extracción con GATE, la cual no está alojada en repositorios públicos.

Antes de compilar o ejecutar el proyecto por primera vez, es **obligatorio** instalar esta librería en el repositorio Maven local de tu máquina. 

Para ello, asegúrate de tener el archivo `.jar` en la raíz del proyecto y ejecuta el siguiente comando en tu terminal:

`mvn install:install-file "-Dfile=utils-1.0-jar-with-dependencies.jar" "-DgroupId=es.uc3m.miaa" "-DartifactId=utils" "-Dversion=1.0" "-Dpackaging=jar"`

## 🚀 Cómo ejecutar el proyecto

El programa lee un archivo de texto plano donde cada línea es una URL (o una ruta a un archivo HTML local) a procesar. 

Abre tu terminal (por ejemplo, PowerShell), navega hasta la carpeta raíz del proyecto (donde se encuentra el archivo `pom.xml`) y ejecuta el siguiente comando:

```powershell
mvn clean compile exec:java "-Dexec.mainClass=org.example.Annotator" "-Dexec.args=urls.txt"