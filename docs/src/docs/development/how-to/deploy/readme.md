---
section: Docs
subsection: Development
chapter: How-To
topic: Deploy the Application
permalink: docs/development/how-to/deploy/
layout: submenu
---

Deploy the Application
======================

Once you have finished customizing the Tile Server and Tile Client, you can deploy and use your new Aperture Tiles application.

<h6 class="procedure">To build your application</h6>

- Execute the following command in the root of your *new-project/* folder:

```bash
gradlew install
```

<h6 class="procedure">To deploy your application</h6>

1. Copy the **new-project.war** to the *webapps/* directory of your web server (e.g., Apache Tomcat or Jetty).
2. Restart the server, if necessary
3. Access your application in web browser at <em>http://localhost:8080/new-project</em>.

## Next Steps ##

For details on using your Aperture Tiles application, see the [User Guide](../../../user-guide/) topic.