package de.opal.installer;

import java.io.File;

public class PatchFileMapping implements Comparable<PatchFileMapping> {
	public File srcFile;
	public File destFile;

	public PatchFileMapping(File srcFile, File destFile) {
		this.srcFile = srcFile;
		this.destFile = destFile;
	}

	public String toString() {
		return "{" + this.srcFile + " -> " + this.destFile + "}";
	}


	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || (this.getClass() != other.getClass())) {
			return false;
		}

		PatchFileMapping guest = (PatchFileMapping) other;
		return (this.destFile.getAbsolutePath() != null
				&& this.destFile.getAbsolutePath().equals(guest.destFile.getAbsolutePath()));
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = 31 * result
				+ (this.destFile.getAbsolutePath() != null ? this.destFile.getAbsolutePath().hashCode() : 0);
		// result = 31*result + (dob !=null ? dob.hashCode() : 0);

		return result;
	}

	@Override
	public int compareTo(PatchFileMapping o) {
		return this.destFile.getAbsolutePath().compareTo(o.destFile.getAbsolutePath());
	}
}
