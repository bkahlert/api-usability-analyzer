package de.fu_berlin.imp.seqan.usability_analyzer.diff;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import de.fu_berlin.imp.seqan.usability_analyzer.diff.model.DiffFile;

public class FileUtils {
	/**
	 * Gets the {@link File} that is described by the passed class's location
	 * and the appended filename.
	 * 
	 * @param clazz
	 * @param filename
	 * @return
	 * @throws URISyntaxException
	 */
	public static File getFile(Class<?> clazz, String filename)
			throws URISyntaxException {
		System.err.println(filename);
		URI uri = clazz.getResource(filename).toURI();
		System.err.println(uri.toString());
		String path = uri.toString().substring(uri.getScheme().length() + 1);
		return new DiffFile(path);
	}

	/**
	 * Gets the {@link File} that is described by the passed class's location
	 * relative to the bin directory.
	 * 
	 * @param filename
	 * @return
	 * @throws URISyntaxException
	 */
	public static File getFile(String filename) throws URISyntaxException {
		StringBuffer sb = new StringBuffer();
		for (int i = 0, num = FileUtils.class.getPackage().getName()
				.split("\\.").length; i < num; i++)
			sb.append("../");
		return getFile(FileUtils.class, sb.toString() + filename);
	}
}
