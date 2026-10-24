# CloudStream Plugin: Hackstore

Plugin oficial y funcional para **CloudStream 3** que extrae catálogo, fichas y enlaces de **Hackstore** (https://hackstore2.com).

## Características
- Películas, Series y Anime en Español Latino, Castellano y Subtitulado.
- Búsqueda integrada rápida por título.
- Soporte para reproductores de vídeo (Vimeos, Streamwish, Filemoon) y servidores de descarga (Mega, Mediafire, Torrents).
- Conexión directa a la API REST de Hackstore2 para máxima velocidad y estabilidad.

## Cómo compilar e instalar en CloudStream

### Método 1: GitHub Actions (Recomendado sin instalar nada)
1. Sube este repositorio a tu cuenta de GitHub.
2. Ve a la pestaña **Actions** en tu repositorio; la compilación se iniciará automáticamente.
3. El plugin compilado (`Hackstore.cs3`) y el repositorio quedarán listos para instalarse agregando la URL de tu repositorio en CloudStream: **Ajustes > Extensiones > Añadir Repositorio**.

### Método 2: Compilación local con Gradle
```bash
git clone https://github.com/tu-usuario/hackstore-cloudstream.git
cd hackstore-cloudstream
./gradlew make
```
El archivo compilado estará en la carpeta `build/`.
