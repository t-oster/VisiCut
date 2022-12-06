# list all targets which are not actual files:
.PHONY: all help jar run libLaserCut clean dist install uninstall prop2po po2prop

PREFIX?=/usr

all: jar

help:
	@echo "\n\n\n\
	usage: \n\
	make (or make jar): compile (includes LibLaserCut) \n\
	make run: compile and run \n\
	make dist: build setup files (in ./distribute subdirectory)\n\
	make clean: remove all compiled files\n\
	"
# Note: If you override the splash screen version with $VERSION, you must run 'make clean' because 'make' doesn't understand the dependency on environment variables.
src/main/resources/de/thomas_oster/visicut/gui/resources/splash.png: splashsource.svg src/main/resources/de/thomas_oster/visicut/gui/resources/VisicutApp.properties
	./generatesplash.sh
jar: src/main/resources/de/thomas_oster/visicut/gui/resources/splash.png libLaserCut
	mvn initialize
	mvn package
dist:
	./distribute/distribute.sh
run: jar
	java -Xmx2048m -Xms256m -jar target/visicut*full.jar
libLaserCut:
	@test -f LibLaserCut/pom.xml  || { echo "Error: the LibLaserCut submodule is missing. Try running 'git submodule update --init'."; false; }
	cd LibLaserCut && mvn install
	cd ..
clean:
	rm -f src/main/resources/de/thomas_oster/visicut/gui/resources/splash.png
	mvn clean
install:
	mkdir -p $(DESTDIR)$(PREFIX)/share/visicut
	cp target/visicut*full.jar $(DESTDIR)$(PREFIX)/share/visicut/Visicut.jar
	mkdir -p $(DESTDIR)$(PREFIX)/share/pixmaps
	cp src/main/resources/de/thomas_oster/visicut/gui/resources/icon.png $(DESTDIR)$(PREFIX)/share/pixmaps/visicut.png
	cp src/main/resources/de/thomas_oster/visicut/gui/resources/icon-48.png $(DESTDIR)$(PREFIX)/share/pixmaps/visicut-48.png
	cp src/main/resources/de/thomas_oster/visicut/gui/resources/icon-32.png $(DESTDIR)$(PREFIX)/share/pixmaps/visicut-32.png
	cp -r distribute/files/* $(DESTDIR)$(PREFIX)/share/visicut/
	mkdir -p $(DESTDIR)$(PREFIX)/share/visicut/inkscape_extension
	cp tools/inkscape_extension/*.inx $(DESTDIR)$(PREFIX)/share/visicut/inkscape_extension/
	cp tools/inkscape_extension/*.py $(DESTDIR)$(PREFIX)/share/visicut/inkscape_extension/
	mkdir -p $(DESTDIR)$(PREFIX)/share/visicut/illustrator_script
	cp tools/illustrator_script/*.scpt $(DESTDIR)$(PREFIX)/share/visicut/illustrator_script/
	mkdir -p $(DESTDIR)$(PREFIX)/bin
	ln -s ../share/visicut/VisiCut.Linux $(DESTDIR)$(PREFIX)/bin/visicut
	mkdir -p $(DESTDIR)$(PREFIX)/share/applications
	cat distribute/linux/VisiCut.desktop | sed s#PREFIX#$(PREFIX)#g# > $(DESTDIR)$(PREFIX)/share/applications/VisiCut.desktop

uninstall:
	rm -rf $(PREFIX)/share/visicut
	rm -f $(PREFIX)/share/pixmaps/visicut.png
	rm -f $(PREFIX)/bin/visicut
	rm -f $(PREFIX)/share/applications/VisiCut.desktop

prop2po:
	prop2po src po

po2prop:
	po2prop --personality=java -t src po src
	find src -name '*.properties' -exec sed -e 's/\(\\u....\)/\L\1/g' -i {} \;
