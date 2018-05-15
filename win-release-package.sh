#!/bin/bash
VERSION=$1
if [ -x ${VERSION} ];
then
	echo VERSION not defined
	exit 1
fi
PACKAGE=taelium-client-${VERSION}
echo PACKAGE="${PACKAGE}"
CHANGELOG=taelium-client-${VERSION}.changelog.txt
OBFUSCATE=$2

FILES="changelogs conf html lib resource contrib logs Jelurida_Public_License.pdf"
FILES="${FILES} taelium.exe taeliumservice.exe"
FILES="${FILES} 3RD-PARTY-LICENSES.txt"
# FILES="${FILES} DEVELOPERS-GUIDE.md OPERATORS-GUIDE.md README.md README.txt USERS-GUIDE.md"
FILES="${FILES} mint.bat mint.sh run.bat run.sh run-tor.sh run-desktop.sh start.sh stop.sh compact.sh compact.bat sign.sh"
FILES="${FILES} taelium.policy taeliumdesktop.policy Dockerfile"

# unix2dos *.bat
echo compile
./win-compile.sh
rm -rf html/doc/*
rm -rf nxt
rm -rf ${PACKAGE}.jar
rm -rf ${PACKAGE}.exe
rm -rf ${PACKAGE}.zip
mkdir -p nxt/
mkdir -p nxt/logs
mkdir -p nxt/addons/src

if [ "${OBFUSCATE}" == "obfuscate" ];
then
echo obfuscate
proguard.bat @nxt.pro
mv ../nxt.map ../nxt.map.${VERSION}
mkdir -p nxt/src/
else
FILES="${FILES} classes src"
FILES="${FILES} compile.sh javadoc.sh jar.sh package.sh"
FILES="${FILES} win-compile.sh win-javadoc.sh win-package.sh"
# echo javadoc
# ./win-javadoc.sh
fi
echo copy resources
cp installer/lib/JavaExe.exe taelium.exe
cp installer/lib/JavaExe.exe taeliumservice.exe
cp -a ${FILES} nxt
cp -a logs/placeholder.txt nxt/logs
echo gzip
for f in `find nxt/html -name *.gz`
do
	rm -f "$f"
done
for f in `find nxt/html -name *.html -o -name *.js -o -name *.css -o -name *.json  -o -name *.ttf -o -name *.svg -o -name *.otf`
do
	gzip -9c "$f" > "$f".gz
done
cd nxt
echo generate jar files
../jar.sh
javapackager -deploy -srcfiles taelium.jar -outdir "OUTDIR"  -outfile "outfile" -appclass nxt.Taelium -native -name "Taelium" -title "Taelium Install"
javapackager -deploy -srcfiles taeliumservice.jar -outdir "OUTDIR"  -outfile "outfile" -appclass nxt.env.service.NxtService -native -name "Taelium Service"
echo package installer Jar
../installer/build-installer.sh ../${PACKAGE}
echo create installer exe
../installer/build-exe.bat ${PACKAGE}
echo create installer zip
cd -
zip -q -X -r ${PACKAGE}.zip nxt -x \*/.idea/\* \*/.gitignore \*/.git/\* \*.iml nxt/conf/nxt.properties nxt/conf/logging.properties nxt/conf/localstorage/\*
rm -rf nxt

# echo creating change log ${CHANGELOG}
# echo -e "Release $1\n" > ${CHANGELOG}
# echo -e "https://bitbucket.org/JeanLucPicard/nxt/downloads/${PACKAGE}.exe\n" >> ${CHANGELOG}
# echo -e "sha256:\n" >> ${CHANGELOG}
# sha256sum ${PACKAGE}.exe >> ${CHANGELOG}

# echo -e "https://bitbucket.org/JeanLucPicard/nxt/downloads/${PACKAGE}.jar\n" >> ${CHANGELOG}
# echo -e "sha256:\n" >> ${CHANGELOG}
# sha256sum ${PACKAGE}.jar >> ${CHANGELOG}

# if [ "${OBFUSCATE}" == "obfuscate" ];
# then
# echo -e "\n\nThis is an experimental release for testing only. Source code is not provided." >> ${CHANGELOG}
# fi
# echo -e "\n\nChange log:\n" >> ${CHANGELOG}

# cat changelogs/${CHANGELOG} >> ${CHANGELOG}
# echo >> ${CHANGELOG}
