package de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.services;

import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.storage.ICodeInstance;

public interface ICodeServiceListener2 extends ICodeServiceListener {
	public void memoModified(ICodeInstance codeInstance);
}
