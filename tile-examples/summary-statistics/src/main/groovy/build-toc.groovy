def buildTocRecursive(groovy.xml.MarkupBuilder html, File directory){
	boolean done = false;
	
	html.ul (id:'listStyleImage') {

		boolean isLeaf = directory.listFiles().findAll({it.isDirectory()}).isEmpty();
		
		if(isLeaf){
			li { a(href:"index.html?dataset=$directory.name", directory.name) }
		}
		else {
			li ("$directory.name")
			directory.eachDir(){ buildTocRecursive(html, it)}
		}
		
	}
}

new File("$project.properties.outputDirectory").mkdirs();
def writer = new FileWriter("$project.properties.outputDirectory/$project.properties.toc")
def htmlBuilder = new groovy.xml.MarkupBuilder(writer)
htmlBuilder.html {
	head {
		title('Table Of Contents')
		link(rel:"stylesheet", href:"css/main.css")
		link(rel:"stylesheet", href:"css/summary.css")
		link(rel:"stylesheet", href:"css/toc.css")
	}
	body {
		div(id:'toc-contents') {
			
			new File(project.properties.dataDir).eachDir(){ topLevel->
				section {
					h1 (topLevel.name)
					buildTocRecursive(htmlBuilder, topLevel)
				}
			}
		}
	}
}

