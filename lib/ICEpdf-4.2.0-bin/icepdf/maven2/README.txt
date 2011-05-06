ICEpdf Maven2 Support

If this is a source distribution, you will NEED to build the jars first so they
are residing in the lib directories of their respective folders!!!

The following utilities are provided:

1) In /maven2 an ant script is provided with the poms for the jars to install to
   whatever local repository you have specified in build.properties file (edit
   build. properties and set the location of the local repository you would like
   to install to).
2) ant target "get-maven" will copy off the internet (make sure you have internet
   access)  the required jar to run maven from ant, to icefaces/lib
3) ant target "install" will install icepdf-core and icepdf-viewer.jar jars
   and poms to your local repository.

This really isn't necessary if you can use the snapshot repository at
http://anonsvn.icefaces.org/repo/maven2/snapshots
Once the release has been made, it will take a day or so for the proper entries
to be accessed.

NOTE: The instructions above assume that you have previously installed maven2.


