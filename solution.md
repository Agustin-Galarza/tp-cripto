
Al correr:

```sh
strings images/budapest.bmp
```

La ultima linea nos dice "al .png cambiar extension por .zip y descomprimir"


Extrayendo la imagen .png del django.bmp encontramos una imagen de un puzzle buscaminas

Si esa imagen la extraemos interpretandola como un archivo zip

```sh
unzip django.png
cat sols/sol4.txt
```

conseguimos las siguientes instrucciones:

```
cada mina es un 1.
cada fila forma una letra.
Los ascii de las letras empiezan todos en 01.
Asi encontraras el algoritmo que tiene clave de 256 bits y el modo
La password esta en otro archivo
Con algoritmo, modo y password hay un .wmv encriptado y oculto.
```

Resolviendo el puzzle y pasandolo a binario conseguimos:


```
000001
100101
110011
000011
100110
100010
```

Agregandole el prefijo `01` y traduciendolo con ASCII:

```
01000001 - A
01100101 - e
01110011 - s
01000011 - C
01100110 - f
01100010 - b
```

```
Aes Cfb
```


