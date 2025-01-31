# Layer With Modification Time (LWMT) JIB Extension

### Purpose

[JIB's](https://github.com/GoogleContainerTools/jib) default layer packaging process causes all files to have their 
timestamps set to `1970-01-01T00:00:01Z`. This is caused due to JIB plugin's 
[reproducibility feature](https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#why-is-my-image-created-48-years-ago) 
and is a good practice for most use cases, but causes problems for our public-facing web services, because it messes up `Last-Modified` headers.
When hosting resources, Spring MVC can read file modification date (or artifact modification date) and use it to answer 
to client's `If-Modified-Since` header. Without this information, browser caching can work incorrectly or less efficiently.

While modification date can be set using a plugin property `filesModificationTime`, it has some drawbacks:
- it can be set only in `all-or-nothing` principle, forcing you to choose between seamless browser caching support and 
  Docker layer caching. Usually we only need one or two files/libraries to have modified time set.
- it can not be set to take the current time, but rather needs to be passed the time as a property. Additionally, it 
  expects a certain time format which not all projects use. In order to support this we would need to introduce new 
  Maven profiles.

This extension aims to achieve the best of both worlds, by enabling users to define which files they want to move from
standard layers (that have modified date set to JIB's default) to a special layer that has last modified time set to 
build time.

This extension is opinionated in a way that it only allows moving the files into a single new layer. Keep in mind that 
changing modification time also changes the layer hash and makes it irreproducible. Therefore, files affected by this
extension should be kept to a minimum and only include the files which are part of the project using this extension.

### Usage

The plugin will:
- Remove files matching the filter from layers and put them in a final layer called `layerWithModificationDate`
- Remove an original layer if it is empty after the files were moved

Here is an example of adding this plugin to a project's plugin management:
```
<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>com.google.cloud.tools</groupId>
                <artifactId>jib-maven-plugin</artifactId>
                <version>${jib-maven-plugin.version}</version>
                <dependencies>
                    <dependency>
                        <groupId>com.infobip.jib</groupId>
                        <artifactId>jib-layer-with-modification-time-extension-maven</artifactId>
                        <version>${jib-layer-with-modification-time-extension-maven.version}</version>
                    </dependency>
                </dependencies>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>dockerBuild</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <pluginExtensions combine.children="append">
                        <pluginExtension>
                            <implementation>
                                com.infobip.jib.extension.lwmt.LayerWithModificationTimeJibExtension
                            </implementation>
                            <configuration implementation="com.infobip.jib.extension.lwmt.Configuration">
                                <filters>
                                    <!-- The following are just examples, make sure to adjust the value for your project -->
                                    <filter>**/resources/**</filter>
                                    <filter>**/static/**</filter>
                                    <filter>**/*${project.parent.artifactId}*.jar</filter>
                                </filters>
                            </configuration>
                        </pluginExtension>
                    </pluginExtensions>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
...
</build>
```

#### Example

If we had a layers organized like this:

```
- layer1
    - a.file
    - b.file
    - c.file
- layer2
    - d.file
    - e.file
- layer3
    - f.file
```

and we ran the plugin with filter `**/a.file`, we'd end up with:

```
- layer1
    - b.file
    - c.file
- layer2
    - d.file
    - e.file
- layer3
    - f.file
- layerWithModificationTime
    - a.file
``` 

while running the plugin with filter `**/f.file` would result in:

```
- layer1
    - a.file
    - b.file
    - c.file
- layer2
    - d.file
    - e.file
- layerWithModificationTime
    - f.file
``` 

### Interesting reads
- [JIB FAQ on reproducible builds](https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#why-is-my-image-created-48-years-ago)
- [Reproducible builds](https://reproducible-builds.org/)
- [GitHub discussion](https://github.com/GoogleContainerTools/jib/issues/2021)

