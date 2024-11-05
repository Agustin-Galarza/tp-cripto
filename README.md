# TPI 72.44 - Criptografía y Seguridad

## Buildear ejecutable

### Usando **GraalVM**

Para utilizar GraalVM, primero se debe tener la versión de Java correspondiente.

Con (SDKMAN)[https://sdkman.io/]:

```sh
sdk install java 21.0.4-graal
```

o usar `sdk list java` para consultar las versiones disponibles.

Teniendo una versión de Java de GraalVM instalada, se puede correr

```sh
./gradlew nativeCompile
```

### Usando **jpackage**

Generar primero el jar utilizando ./gradlew jar

Luego correr:

```bash
jpackage \
--input app/build/libs \
--name stegobmp \
--main-jar app.jar \
--main-class ar.edu.itba.Main \
--type app-image \
--dest <output-path>
```
