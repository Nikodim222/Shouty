https://stackoverflow.com/questions/4115471/could-not-find-main-method-from-given-launch-configuration-error-when-exportin

just had the same problem :s it's annoying but easy to manually fix in the manifest file.

open the .jar file with winrar or 7zip.
locate the manifest file (META-INF folder)
change it to this.
Manifest-Version: 1.0
 main-class: (package).(main-class)

In my case i had it in the default package.

Manifest-Version: 1.0
main-class: ru.home.StartIt

./META-INF/MANIFEST.MF should be as follows below,

Manifest-Version: 1.0
Class-Path: .
main-class: ru.home.StartIt
