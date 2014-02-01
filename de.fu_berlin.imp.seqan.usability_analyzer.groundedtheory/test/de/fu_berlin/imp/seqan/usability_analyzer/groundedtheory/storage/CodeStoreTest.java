package de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.bkahlert.devel.nebula.colors.RGB;
import com.bkahlert.devel.nebula.utils.ExecutorUtil;

import de.fu_berlin.imp.seqan.usability_analyzer.core.model.IdentifierFactory;
import de.fu_berlin.imp.seqan.usability_analyzer.core.model.TimeZoneDate;
import de.fu_berlin.imp.seqan.usability_analyzer.core.model.TimeZoneDateRange;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.model.Code;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.model.Episode;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.model.ICode;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.storage.exceptions.CodeDoesNotExistException;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.storage.exceptions.CodeHasChildCodesException;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.storage.exceptions.CodeInstanceDoesNotExistException;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.storage.exceptions.CodeStoreFullException;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.storage.exceptions.CodeStoreReadException;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.storage.exceptions.CodeStoreWriteAbandonedCodeInstancesException;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.storage.exceptions.CodeStoreWriteException;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.storage.impl.CodeStoreHelper;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.storage.impl.DuplicateCodeException;
import de.fu_berlin.imp.seqan.usability_analyzer.groundedtheory.storage.impl.DuplicateCodeInstanceException;

public class CodeStoreTest extends CodeStoreHelper {

	@Rule
	public JUnitRuleMockery context = new JUnitRuleMockery() {
		{
			this.setThreadingPolicy(new Synchroniser());
		}
	};

	public CodeStoreTest() throws IOException, URISyntaxException {
		super();
	}

	@Test(expected = CodeStoreReadException.class)
	public void testNonExistingLoadCodes() throws IOException,
			SAXParseException {
		this.getNonExistingCodeStore().getTopLevelCodes();
	}

	@Test(expected = CodeStoreReadException.class)
	public void testNonExistingLoadCodeInstances() throws IOException,
			SAXParseException {
		this.getNonExistingCodeStore().loadInstances();
	}

	@Test
	public void testEmptyLoadCodes() throws IOException, SAXParseException {
		List<ICode> loadedCodes = this.getEmptyCodeStore().getTopLevelCodes();
		assertEquals(0, loadedCodes.size());
	}

	@Test
	public void testEmptyLoadCodeInstances() throws IOException,
			SAXParseException {
		Set<ICodeInstance> loadedCodeInstances = this.getEmptyCodeStore()
				.loadInstances();
		assertEquals(0, loadedCodeInstances.size());
	}

	@Test
	@Ignore
	public void testGetCodes() {
		assertTrue(false);
	}

	@Test
	public void testLoadCodes() throws IOException, DuplicateCodeException,
			DuplicateCodeInstanceException {
		ICodeStore codeStore = this.getEmptyCodeStore();
		codeStore.addAndSaveCode(this.code1);
		codeStore.addAndSaveCode(this.code2);
		codeStore
				.addAndSaveCodeInstances(new ICodeInstance[] { this.codeInstance1 });
		codeStore.addAndSaveCodeInstances(new ICodeInstance[] {
				this.codeInstance2, this.codeInstance3 });
		codeStore.save();
		this.testCodes(this.getSmallCodeStore(), this.codes);
	}

	@Test
	public void testLoadCodeInstances() throws IOException, SAXParseException {
		this.testCodeInstances(this.getSmallCodeStore(), this.codeInstances);
	}

	@Test
	public void testNewFileSave() throws IOException, SAXException,
			ParserConfigurationException, DuplicateCodeException,
			DuplicateCodeInstanceException {
		ICodeStore newCodeStore = this.getEmptyCodeStore();
		for (ICode code : this.codes) {
			newCodeStore.addAndSaveCode(code);
		}
		for (ICodeInstance instance : this.codeInstances) {
			newCodeStore
					.addAndSaveCodeInstances(new ICodeInstance[] { instance });
		}
		this.testCodes(newCodeStore, this.codes);
		this.testCodeInstances(newCodeStore, this.codeInstances);
	}

	@Test
	public void testNewFileSaveCodes() throws IOException, SAXException,
			ParserConfigurationException, DuplicateCodeException {
		ICodeStore newCodeStore = this.getEmptyCodeStore();
		for (ICode code : this.codes) {
			newCodeStore.addAndSaveCode(code);
		}
		newCodeStore.save();
		this.testCodes(newCodeStore, this.codes);
	}

	@Test
	public void testNewFileSaveCodeInstances() throws IOException,
			DuplicateCodeException, DuplicateCodeInstanceException {
		ICodeStore newCodeStore = this.getEmptyCodeStore();
		newCodeStore.addAndSaveCode(this.code2);
		newCodeStore.addAndSaveCodeInstances(new ICodeInstance[] {
				this.codeInstance1, this.codeInstance3 });

		assertEquals(2, newCodeStore.loadInstances().size());
		this.testCodeInstances(newCodeStore, new ICodeInstance[] {
				this.codeInstance1, this.codeInstance3 });
	}

	@Test(expected = CodeStoreWriteException.class)
	public void testNewFileSaveCodeInstancesWithoutCodes() throws IOException,
			SAXException, ParserConfigurationException,
			DuplicateCodeInstanceException {
		ICodeStore newCodeStore = this.getEmptyCodeStore();
		newCodeStore.addAndSaveCodeInstances(new ICodeInstance[] {
				this.codeInstance1, this.codeInstance3, this.codeInstance2 });
		newCodeStore.save();
	}

	@Test
	public void testNewFileAddAndSaveCodeInstances() throws IOException,
			DuplicateCodeException, DuplicateCodeInstanceException {
		ICodeStore newCodeStore = this.getEmptyCodeStore();
		newCodeStore.addAndSaveCode(this.code2);
		newCodeStore
				.addAndSaveCodeInstances(new ICodeInstance[] { this.codeInstance1 });
		newCodeStore
				.addAndSaveCodeInstances(new ICodeInstance[] { this.codeInstance3 });

		assertEquals(2, newCodeStore.loadInstances().size());
		this.testCodeInstances(newCodeStore, new ICodeInstance[] {
				this.codeInstance1, this.codeInstance3 });

		newCodeStore.addAndSaveCode(this.code2);
		assertEquals(2, newCodeStore.loadInstances().size());
		this.testCodeInstances(newCodeStore, new ICodeInstance[] {
				this.codeInstance1, this.codeInstance3 });
	}

	@Test
	public void testSmallFileSaveCodes() throws IOException,
			DuplicateCodeException {
		ICodeStore codeStore = this.getSmallCodeStore();

		this.testCodes(codeStore, this.codes);
		this.testCodeInstances(codeStore, this.codeInstances);

		ICode code3 = new Code(42l, "solution", RGB.WHITE, new TimeZoneDate());

		codeStore.addAndSaveCode(code3);
		assertEquals(3, codeStore.getTopLevelCodes().size());
		this.testCodes(codeStore, new ICode[] { this.code1, this.code2, code3 });
		assertEquals(3, codeStore.loadInstances().size());
		this.testCodeInstances(codeStore, this.codeInstances);
	}

	@Test(expected = CodeStoreWriteAbandonedCodeInstancesException.class)
	public void testSmallFileSaveCodesMakingInstancesInvalid()
			throws IOException, CodeHasChildCodesException,
			CodeDoesNotExistException {
		ICodeStore codeStore = this.getSmallCodeStore();
		this.testCodes(codeStore, this.codes);
		this.testCodeInstances(codeStore, this.codeInstances);

		codeStore.removeAndSaveCode(this.code1);
	}

	@Test
	public void testSmallFileSaveCodeInstances() throws IOException,
			CodeHasChildCodesException, CodeDoesNotExistException,
			DuplicateCodeInstanceException {
		ICodeStore codeStore = this.getSmallCodeStore();
		this.testCodes(codeStore, this.codes);
		this.testCodeInstances(codeStore, this.codeInstances);

		codeStore.removeAndSaveCodeInstance(this.codeInstance2);
		assertEquals(2, codeStore.getTopLevelCodes().size());
		this.testCodes(codeStore, this.codes);
		assertEquals(2, codeStore.loadInstances().size());
		this.testCodeInstances(codeStore, new ICodeInstance[] {
				this.codeInstance1, this.codeInstance3 });

		codeStore.removeAndSaveCode(this.code1);
		assertEquals(1, codeStore.getTopLevelCodes().size());
		this.testCodes(codeStore, new ICode[] { this.code2 });
		assertEquals(2, codeStore.loadInstances().size());
		this.testCodeInstances(codeStore, new ICodeInstance[] {
				this.codeInstance1, this.codeInstance3 });

		codeStore.removeAndSaveCodeInstance(this.codeInstance3);
		assertEquals(1, codeStore.getTopLevelCodes().size());
		this.testCodes(codeStore, new ICode[] { this.code2 });
		assertEquals(1, codeStore.loadInstances().size());
		this.testCodeInstances(codeStore,
				new ICodeInstance[] { this.codeInstance1 });

		codeStore
				.addAndSaveCodeInstances(new ICodeInstance[] { this.codeInstance3 });
		assertEquals(1, codeStore.getTopLevelCodes().size());
		this.testCodes(codeStore, new ICode[] { this.code2 });
		assertEquals(2, codeStore.loadInstances().size());
		this.testCodeInstances(codeStore, new ICodeInstance[] {
				this.codeInstance1, this.codeInstance3 });
	}

	@Test(expected = CodeStoreWriteAbandonedCodeInstancesException.class)
	public void testSmallFileSaveCodeInstancesMakingInstancesInvalid()
			throws IOException, CodeHasChildCodesException,
			CodeDoesNotExistException, DuplicateCodeInstanceException {
		ICodeStore codeStore = this.getSmallCodeStore();
		this.testCodes(codeStore, this.codes);
		this.testCodeInstances(codeStore, this.codeInstances);

		codeStore.removeAndSaveCodeInstance(this.codeInstance2);
		assertEquals(2, codeStore.getTopLevelCodes().size());
		this.testCodes(codeStore, this.codes);
		assertEquals(2, codeStore.loadInstances().size());
		this.testCodeInstances(codeStore, new ICodeInstance[] {
				this.codeInstance1, this.codeInstance3 });

		codeStore.removeAndSaveCode(this.code1);
		assertEquals(1, codeStore.getTopLevelCodes().size());
		this.testCodes(codeStore, new ICode[] { this.code2 });
		assertEquals(2, codeStore.loadInstances().size());
		this.testCodeInstances(codeStore, new ICodeInstance[] {
				this.codeInstance1, this.codeInstance3 });

		codeStore
				.addAndSaveCodeInstances(new ICodeInstance[] { this.codeInstance1 });
		codeStore.addAndSaveCodeInstances(new ICodeInstance[] {
				this.codeInstance2, this.codeInstance3 });
	}

	@Test
	public void testCreateCode() throws IOException, CodeStoreFullException {
		ICodeStore codeStore = this.getEmptyCodeStore();
		assertEquals(0, codeStore.getTopLevelCodes().size());
		assertEquals(0, codeStore.loadInstances().size());

		ICode code = codeStore.createCode("Code #1", RGB.WHITE);
		assertEquals(Long.MIN_VALUE, code.getId());
		assertEquals("Code #1", code.getCaption());
	}

	@Test
	public void testNonExistingCreateCode() throws IOException,
			CodeStoreFullException, IllegalArgumentException,
			DuplicateCodeException {
		ICodeStore codeStore = this.getEmptyCodeStore();
		assertEquals(0, codeStore.getTopLevelCodes().size());
		assertEquals(0, codeStore.loadInstances().size());

		assertEquals(new Code(Long.MIN_VALUE, "Code #1", RGB.WHITE,
				new TimeZoneDate()), codeStore.createCode("Code #1", RGB.WHITE));
		assertEquals(new Code(Long.MIN_VALUE + 1, "Code #2", new RGB(1.0, 1.0,
				1.0), new TimeZoneDate()), codeStore.createCode("Code #2",
				RGB.WHITE));
		codeStore.addAndSaveCode(new Code(5l, "Code #3", RGB.WHITE,
				new TimeZoneDate()));
		assertEquals(new Code(6l, "Code #4", RGB.WHITE, new TimeZoneDate()),
				codeStore.createCode("Code #4", new RGB(1.0, 1.0, 1.0)));
	}

	@Test
	public void testEmptyCreateCode() throws IOException,
			CodeStoreFullException, IllegalArgumentException,
			DuplicateCodeException {
		ICodeStore codeStore = this.getEmptyCodeStore();
		assertEquals(0, codeStore.getTopLevelCodes().size());
		assertEquals(0, codeStore.loadInstances().size());

		assertEquals(new Code(Long.MIN_VALUE, "Code #1", RGB.WHITE,
				new TimeZoneDate()), codeStore.createCode("Code #1", RGB.WHITE));
		assertEquals(new Code(Long.MIN_VALUE + 1, "Code #2", new RGB(1.0, 1.0,
				1.0), new TimeZoneDate()), codeStore.createCode("Code #2",
				RGB.WHITE));
		codeStore.addAndSaveCode(new Code(5l, "Code #3", RGB.WHITE,
				new TimeZoneDate()));
		assertEquals(new Code(6l, "Code #4", RGB.WHITE, new TimeZoneDate()),
				codeStore.createCode("Code #4", new RGB(1.0, 1.0, 1.0)));
	}

	@Test
	public void testSmallCreateCode() throws IOException,
			CodeStoreFullException, IllegalArgumentException,
			DuplicateCodeException {
		ICodeStore codeStore = this.getSmallCodeStore();
		this.testCodes(codeStore, this.codes);
		this.testCodeInstances(codeStore, this.codeInstances);

		assertEquals(new Code(234233209l + 1l, "Code #1", RGB.WHITE,
				new TimeZoneDate()), codeStore.createCode("Code #1", RGB.WHITE));
		assertEquals(new Code(234233209l + 2l, "Code #2", RGB.WHITE,
				new TimeZoneDate()), codeStore.createCode("Code #2", RGB.WHITE));
		codeStore.addAndSaveCode(new Code(300000000l, "Code #3", new RGB(1.0,
				1.0, 1.0), new TimeZoneDate()));
		assertEquals(new Code(300000001l, "Code #4", RGB.WHITE,
				new TimeZoneDate()), codeStore.createCode("Code #4", new RGB(
				1.0, 1.0, 1.0)));
	}

	@Test(expected = CodeStoreFullException.class)
	public void testOverflowCreateCode() throws IOException,
			CodeStoreFullException, IllegalArgumentException,
			DuplicateCodeException {
		ICodeStore codeStore = this.getSmallCodeStore();
		this.testCodes(codeStore, this.codes);
		this.testCodeInstances(codeStore, this.codeInstances);

		codeStore.addAndSaveCode(new Code(Long.MAX_VALUE, "Code #1", new RGB(
				1.0, 1.0, 1.0), new TimeZoneDate()));
		codeStore.createCode("Code #2", RGB.WHITE);
	}

	@Test(expected = InvalidParameterException.class)
	public void testNonExistingCreateCodeInstance() throws IOException,
			InvalidParameterException, DuplicateCodeInstanceException,
			URISyntaxException, CodeStoreFullException {
		final ICode code = this.context.mock(ICode.class);

		ICodeStore codeStore = this.getEmptyCodeStore();
		assertEquals(0, codeStore.getTopLevelCodes().size());
		assertEquals(0, codeStore.loadInstances().size());

		codeStore.createCodeInstances(new ICode[] { code },
				new URI[] { new URI("abc://def") });
	}

	@Test(expected = InvalidParameterException.class)
	public void testEmptyCreateCodeInstance() throws IOException,
			InvalidParameterException, DuplicateCodeInstanceException,
			URISyntaxException, CodeStoreFullException {
		final ICode code = this.context.mock(ICode.class);

		ICodeStore codeStore = this.getEmptyCodeStore();
		assertEquals(0, codeStore.getTopLevelCodes().size());
		assertEquals(0, codeStore.loadInstances().size());

		codeStore.createCodeInstances(new ICode[] { code },
				new URI[] { new URI("abc://def") });
	}

	@Test
	public void testSmallCreateCodeInstance() throws IOException,
			InvalidParameterException, DuplicateCodeInstanceException,
			URISyntaxException, CodeStoreFullException {
		ICodeStore codeStore = this.getSmallCodeStore();
		this.testCodes(codeStore, this.codes);
		this.testCodeInstances(codeStore, this.codeInstances);

		final ICodeInstance codeInstance = codeStore.createCodeInstances(
				new ICode[] { this.code1 },
				new URI[] { new URI("sua://new_id") })[0];
		codeStore.addAndSaveCodeInstances(new ICodeInstance[] { codeInstance });
		this.testCodes(codeStore, this.codes);
		this.testCodeInstances(codeStore, new ICodeInstance[] {
				this.codeInstance1, this.codeInstance2, this.codeInstance3,
				codeInstance });
	}

	@Test
	public void testLoadInstances() throws IOException {
		ICodeStore codeStore = this.getSmallCodeStore();
		this.testCodeInstances(codeStore.loadInstances(), this.codeInstances);
	}

	@Test(expected = CodeInstanceDoesNotExistException.class)
	public void testSmallDeleteInexistingCodeInstance() throws Exception {
		final ICodeInstance codeInstance = this.context
				.mock(ICodeInstance.class);
		this.context.checking(new Expectations() {
			{
				this.allowing(codeInstance).getId();
				this.will(returnValue("my_id"));
			}
		});

		ICodeStore codeStore = this.getSmallCodeStore();
		this.testCodes(codeStore, this.codes);
		this.testCodeInstances(codeStore, this.codeInstances);

		codeStore.deleteCodeInstance(codeInstance);
	}

	@Test
	public void testSmallDeleteExistingCodeInstance() throws Exception {
		ICodeStore codeStore = this.getSmallCodeStore();

		this.testCodes(codeStore, new ICode[] { this.code1, this.code2 });
		this.testCodeInstances(codeStore, new ICodeInstance[] {
				this.codeInstance1, this.codeInstance2, this.codeInstance3 });

		codeStore.deleteCodeInstance(this.codeInstance1);

		this.testCodes(codeStore, new ICode[] { this.code1, this.code2 });
		this.testCodeInstances(codeStore, new ICodeInstance[] {
				this.codeInstance2, this.codeInstance3 });

		codeStore.deleteCodeInstance(this.codeInstance2);

		this.testCodes(codeStore, new ICode[] { this.code1, this.code2 });
		this.testCodeInstances(codeStore,
				new ICodeInstance[] { this.codeInstance3 });

		codeStore.deleteCodeInstance(this.codeInstance3);

		this.testCodes(codeStore, new ICode[] { this.code1, this.code2 });
		this.testCodeInstances(codeStore, new ICodeInstance[] {});
	}

	@Test
	public void testEmptyDeleteCodeInstances() throws IOException,
			InvalidParameterException, DuplicateCodeInstanceException {
		final ICode code = this.context.mock(ICode.class);
		this.context.checking(new Expectations() {
			{
				this.allowing(code).getId();
				this.will(returnValue(CodeStoreTest.this.code1.getId()));
			}
		});

		ICodeStore codeStore = this.getEmptyCodeStore();
		assertEquals(0, codeStore.getTopLevelCodes().size());
		assertEquals(0, codeStore.loadInstances().size());

		codeStore.deleteCodeInstances(code);

		assertEquals(0, codeStore.getTopLevelCodes().size());
		assertEquals(0, codeStore.loadInstances().size());
	}

	@Test
	public void testSmallDeleteInexistingCodeInstances() throws IOException,
			InvalidParameterException, DuplicateCodeInstanceException {
		final ICode code = this.context.mock(ICode.class);
		this.context.checking(new Expectations() {
			{
				this.allowing(code).getId();
				this.will(returnValue(-1l));
			}
		});

		ICodeStore codeStore = this.getSmallCodeStore();
		this.testCodes(codeStore, this.codes);
		this.testCodeInstances(codeStore, this.codeInstances);

		codeStore.deleteCodeInstances(code);

		this.testCodes(codeStore, this.codes);
		this.testCodeInstances(codeStore, this.codeInstances);
	}

	@Test
	public void testSmallDeleteExistingCodeInstances() throws IOException,
			InvalidParameterException, DuplicateCodeInstanceException {
		ICodeStore codeStore = this.getSmallCodeStore();
		this.testCodes(codeStore, new ICode[] { this.code1, this.code2 });
		this.testCodeInstances(codeStore, new ICodeInstance[] {
				this.codeInstance1, this.codeInstance2, this.codeInstance3 });

		codeStore.deleteCodeInstances(this.code2);

		this.testCodes(codeStore, new ICode[] { this.code1, this.code2 });
		this.testCodeInstances(codeStore,
				new ICodeInstance[] { this.codeInstance2 });
	}

	@Test(expected = CodeDoesNotExistException.class)
	public void testEmptyDeleteInexistingCode() throws Exception {
		final ICode code = this.context.mock(ICode.class);
		this.context.checking(new Expectations() {
			{
				this.allowing(code).getId();
				this.will(returnValue(-1l));
			}
		});

		ICodeStore codeStore = this.getEmptyCodeStore();
		assertEquals(0, codeStore.getTopLevelCodes().size());
		assertEquals(0, codeStore.loadInstances().size());

		codeStore.removeAndSaveCode(code);
	}

	@Test
	public void testSmallDeleteExistingCode() throws Exception {
		ICodeStore codeStore = this.getSmallCodeStore();
		this.testCodes(codeStore, new ICode[] { this.code1, this.code2 });
		this.testCodeInstances(codeStore, new ICodeInstance[] {
				this.codeInstance1, this.codeInstance2, this.codeInstance3 });

		codeStore.removeAndSaveCode(this.code2, true);

		this.testCodes(codeStore, new ICode[] { this.code1 });
		this.testCodeInstances(codeStore,
				new ICodeInstance[] { this.codeInstance2 });
	}

	@Test
	public void testSaveMemo() throws Exception {
		ICodeStore codeStore = this.getSmallCodeStore();
		codeStore.setMemo(this.codeInstance2, "abc");
		assertEquals("abc", codeStore.getMemo(this.codeInstance2));
		codeStore.setMemo(this.code1, "äöü´ß ^°∞ 和平");
		assertEquals("äöü´ß ^°∞ 和平", codeStore.getMemo(this.code1));
		codeStore.setMemo(this.locatable2.getUri(), "line1\nline2\nline3");
		assertEquals("line1\nline2\nline3",
				codeStore.getMemo(this.locatable2.getUri()));

		ICodeStore codeStore2 = this.loadFromCodeStore(codeStore);
		assertEquals("abc", codeStore2.getMemo(this.codeInstance2));
		assertEquals("äöü´ß ^°∞ 和平", codeStore2.getMemo(this.code1));
		assertEquals("line1\nline2\nline3",
				codeStore2.getMemo(this.locatable2.getUri()));

		// TODO ???
		// System.err.println(this.getTextFromStyledTextWidget("äöü´ß ^°∞ 和平"));

		codeStore.removeAndSaveCode(this.code1, true);
		assertNull(codeStore.getMemo(this.code1));
		assertNull(codeStore.getMemo(this.codeInstance2));
		assertEquals("line1\nline2\nline3",
				codeStore2.getMemo(this.locatable2.getUri()));

		codeStore.setMemo(this.locatable2.getUri(), null);
		assertNull(codeStore2.getMemo(this.locatable2.getUri()));
	}

	public void testSaveEpisode() throws IOException {
		ICodeStore codeStore = this.getSmallCodeStore();
		assertEquals(0, codeStore.getEpisodes().size());

		Episode episode = new Episode(IdentifierFactory.createFrom("id"),
				new TimeZoneDateRange(new TimeZoneDate(
						"2000-01-02T14:00:00.000+02:00"), new TimeZoneDate(
						"2000-01-02T14:30:00.000+02:00")), "TimelineViewer");
		codeStore.getEpisodes().add(episode);
		assertEquals(1, codeStore.getEpisodes().size());
		assertEquals(episode, codeStore.getEpisodes().iterator().next());

		ICodeStore codeStore2 = this.loadFromCodeStore(codeStore);
		assertEquals(1, codeStore2.getEpisodes().size());
		assertEquals(episode, codeStore2.getEpisodes().iterator().next());

		codeStore2.getEpisodes().remove(episode);
		assertEquals(0, codeStore2.getEpisodes().size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSaveEpisodeIllegalArgumentException() throws IOException {
		this.getSmallCodeStore().getEpisodes().add(null);
	}

	@SuppressWarnings("unused")
	private String getTextFromStyledTextWidget(String text) {
		Display display = new Display();
		final Shell shell = new Shell(display);

		shell.setLayout(new GridLayout());

		final StyledText styledText = new StyledText(shell, SWT.MULTI
				| SWT.WRAP | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

		styledText.setLayoutData(new GridData(GridData.FILL_BOTH));

		Font font = new Font(shell.getDisplay(), "Courier New", 12, SWT.NORMAL);
		styledText.setFont(font);

		styledText.setText(text);
		final AtomicReference<String> returnedText = new AtomicReference<String>();
		styledText.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				returnedText.set(styledText.getText());
			}
		});

		shell.setSize(300, 120);
		shell.open();

		ExecutorUtil.asyncExec(new Runnable() {
			@Override
			public void run() {
				shell.close();
			}
		}, 500);

		// Set up the event loop.
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				// If no more entries in event queue
				display.sleep();
			}
		}

		// waiting for other thread to close the shell and make the code
		// continue here

		return returnedText.get();
	}
}
