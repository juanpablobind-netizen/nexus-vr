# NEXUS VR • 0.1.0
Prototipo Android nativo para teléfonos dentro de visores VR de tipo Cardboard / VR Box. Interfaz original en español. Código Java, CameraX, MediaPipe y Media3. No es el sistema de Meta ni una aplicación oficial de Google Cardboard.

## Compilar en Windows
1. Instala Android Studio desde https://developer.android.com/studio y completa la configuración inicial. En SDK Manager instala Android SDK Platform 35 y Android SDK Build-Tools 35.0.0. Usa JDK 17 o un JDK compatible con Gradle 8.9 (el script intenta localizar el de Android Studio).
2. Descomprime todo el ZIP. Abre `COMPILAR-WINDOWS.bat`. El script descarga Gradle 8.9, genera el wrapper y compila el APK debug. Gradle descarga las bibliotecas y el modelo de manos oficial (se incorpora al APK). Hace falta conexión a internet durante la compilación.
3. Si termina correctamente, encontrarás `app/build/outputs/apk/debug/app-debug.apk`. Puedes copiarlo al teléfono e instalarlo autorizando esa fuente de instalación, o ejecutar el proyecto desde Android Studio después de generar el wrapper. No necesita publicar en Play Store.
4. Alternativa para desarrolladores: usa Gradle 8.9 y ejecuta `gradle :app:assembleDebug` con Android SDK 35 y JDK 17. También se incluye una configuración de GitHub Actions que puedes ejecutar en un repositorio propio para compilar el APK; no se ha ejecutado aquí.

## Primer uso
- Android 7.0 o posterior, preferentemente dispositivo ARM64 con giroscopio, cámara trasera y buen rendimiento. Los dispositivos y visores no han sido probados. La cámara trasera debe quedar descubierta y apuntar hacia tus manos.
- Inicia la app, permite la cámara, sostén el móvil horizontalmente en la orientación del visor y centra la vista con un toque prolongado antes de colocarlo dentro. También hay un botón Centrar en 5 s para colocar el teléfono y ajuste IPD virtual en Ajustes. No es una calibración óptica real.
- Mueve la cabeza para mirar la ventana; si se pierde, mantén pulsada cualquier zona de la pantalla o toca la franja inferior para centrar. Si no puedes acceder a la pantalla, retira el teléfono del visor.
- Sin manos detectadas, mira un botón aproximadamente 1,4 segundos. Con la mano visible, apunta con el índice y junta pulgar e índice para seleccionar; suelta para la siguiente selección. Pellizca la barra superior y mueve la mano para arrastrar; pellizca con ambas manos y separa o acerca para cambiar tamaño. También se puede tocar la pantalla directamente.
- En Navegador / URL puedes abrir sitios HTTPS. La entrada de texto usa el teclado del teléfono: retíralo del visor para escribir. No se garantiza compatibilidad con todas las páginas, DRM o reproducción web.
- Multimedia acepta archivos locales mediante el selector Android o URLs HTTPS de video/audio. IPTV acepta listas M3U locales y por HTTPS; las entradas HTTP se muestran pero no se reproducen por la política de red segura. Solo usa contenido propio o autorizado. No incluye listas ni canales. Si la URL es un manifiesto HLS .m3u8 de video, ábrelo como multimedia, no como una lista IPTV.
- Entornos permite cambiar el paisaje gráfico y activar vista monocular de cámara experimental. La imagen es la misma para ambos ojos; no tiene profundidad real ni alineación óptica calibrada.

## Alcance y limitaciones
- Renderizado estereoscópico propio de paneles proyectados en 3D, no SDK Cardboard. No hay corrección de distorsión de lentes, seguimiento posicional 6DoF, límites de seguridad, calibración individual de cámaras o reconstrucción 3D de manos. El cursor usa puntos 2D de MediaPipe; la orientación de cabeza es 3DoF. No hay anclaje espacial físico ni sistema operativo virtual.
- Una ventana principal con navegación entre aplicaciones, no ventanas múltiples independientes. Los entornos son gráficos procedurales sencillos; navegador y video se copian a la ventana, por lo que la tasa de imagen puede ser baja y el contenido protegido puede no verse. Sin escritorio remoto. No se promete 60 FPS.
- Cámara procesada localmente sin grabación ni subida por la app. La red se utiliza para el navegador y las fuentes multimedia solicitadas. Usa el visor sentado y despejado; no camines con la vista de cámara. Detente si aparece mareo, dolor de cabeza o fatiga visual.
- Estado de entrega: código fuente, no APK. El entorno de creación no tenía Android SDK ni acceso de red para descargar dependencias/modelo; no se ejecutó Gradle ni se probó en Android. Es un prototipo que puede necesitar ajustes de compilación o de hardware. El importador M3U sí se comprobó con una prueba Java de 6000 entradas sintéticas, sin verificar reproducción de canales.

## Referencias técnicas
- MediaPipe: https://developers.google.com/edge/mediapipe/solutions/vision/hand_landmarker/android
- CameraX: https://developer.android.com/media/camera/camerax/analyze
- Media3: https://developer.android.com/media/media3/exoplayer
- Gradle/AGP: https://developer.android.com/build/releases/about-agp
