all: compile

compile:
	java -jar jtb132di.jar -te minijava.jj
	java -jar javacc5.jar minijava-jtb.jj
	javac Main.java

run:
	java Main Example.java BinaryTree.java BubbleSort.java BubbleSort-error.java Factorial.java Factorial-error.java LinearSearch.java LinearSearch-error.java LinkedList.java LinkedList-error.java MoreThan4.java QuickSort.java QuickSort-error.java TreeVisitor.java TreeVisitor-error.java
	java Main BadAssign.java BadAssign2.java DoubleDeclaration1.java DoubleDeclaration4.java DoubleDeclaration6.java test18.java test21.java test35.java test52.java UseArgs.java

clean:
	rm -f *.class *~
