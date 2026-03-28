plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.neuralsound"
            artifactId = "ffmpeg-kit"
            version = "1.0.0"

            artifact(file("libs/ffmpeg-kit.aar"))

            pom {
                packaging = "aar"
                withXml {
                    val deps = asNode().appendNode("dependencies")
                    val dep = deps.appendNode("dependency")
                    dep.appendNode("groupId", "com.arthenica")
                    dep.appendNode("artifactId", "smart-exception-java")
                    dep.appendNode("version", "0.2.1")
                    dep.appendNode("scope", "runtime")
                }
            }
        }
    }
}
