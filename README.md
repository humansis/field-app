# Offline app


## CI/CD Workflow
The current workflow is following: 

TBD

It's very important to use properly versioning in user app in file `app/build.gradle`

```
	// Major updates are non-compatible, meaning consumers can not upgrade without changing their software where applicable.
	def VERSION_MAJOR = 1
	// Minor updates are backward compatible, meaning consumers can upgrade freely. Main for non-breaking new functionality.
	def VERSION_MINOR = 0
	// Patch updates are interchangeable, meaning consumers can upgrade or downgrade freely. Mainly for bug fixing.
	def VERSION_PATCH = 0
```


## Important links
 - **Artifactory repository**
 	- https://artifactory.quanti.cz/ui/repos/tree/General/user_app-gradle-local
- **Direct file download**
	- https://artifactory.quanti.cz:443/artifactory/user_app-gradle-local/

Name and password are LDAP (so same as JIRA / Gitlab / Nextclous)
