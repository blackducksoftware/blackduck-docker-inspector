package com.blackducksoftware.integration.hub.docker

import java.io.File
import org.apache.commons.io.FileUtils
import org.springframework.stereotype.Component
import com.blackducksoftware.integration.hub.docker.tar.TarExtractionResult

@Component
class PackageManagerFiles {
	public void stubPackageManagerFiles(TarExtractionResult result){
		File packageManagerDirectory = new File(result.packageManager.directory)
		if(packageManagerDirectory.exists()){
			deleteFilesOnly(packageManagerDirectory)
			if(result.packageManager == PackageManagerEnum.DPKG){
				File statusFile = new File(packageManagerDirectory, 'status')
				statusFile.createNewFile()
				File updatesDir = new File(packageManagerDirectory, 'updates')
				updatesDir.mkdir()
			}
		}
		FileUtils.copyDirectory(result.extractedPackageManagerDirectory, packageManagerDirectory)
	}
	
	private void deleteFilesOnly(File file){
		if (file.isDirectory()){
			for (File subFile: file.listFiles()) {
				deleteFilesOnly(subFile)
			}
		} else{
			file.delete()
		}
	}
}
