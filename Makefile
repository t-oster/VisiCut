PREFIX?=/usr/local
all: jar

jar:
	ant jar
clean:
	ant clean
install: jar
	mkdir -p $(PREFIX)/share/visicut/
	cp -r dist/* $(PREFIX)/share/visicut/
	mkdir -p $(PREFIX)/share/pixmaps
	cp icon.png $(PREFIX)/share/pixmaps/visicut.png
	cp distribute/linux/visicut $(PREFIX)/share/visicut/
	cp -r distribute/files/* $(PREFIX)/share/visicut/
	ln -s $(PREFIX)/share/visicut/visicut $(PREFIX)/bin/visicut
	mkdir -p $(PREFIX)/share/applications
	cat distribute/linux/VisiCut.desktop | sed s#PREFIX#$(PREFIX)#g# > $(PREFIX)/share/applications/VisiCut.desktop

uninstall:
	rm -rf $(PREFIX)/share/visicut
	rm -f $(PREFIX)/share/pixmaps/visicut.png
	rm -f $(PREFIX)/bin/visicut
	rm -f $(PREFIX)/share/applications/VisiCut.desktop
