List Extractor for English
==========================

#Usage

The program uses ClearNLP API which can be found at 'http://clearnlp.com'. Make sure you include all the dependencies in the classpath while compiling. 

The following is the recommended practice:

Place the ClearNLP jar files in './clearnlp'.

From the root directory use

'''javac -d lib -cp ":lib:clearnlp/*" java/listextractor/<file-name>.java''' 

to compile the java code.

From the lib directory use

'''java -cp ":../clearnlp/*" -Xmx4G -XX:+UseConcMarkSweepGC listextractor.<class-name>'''

to run the compiled classes.