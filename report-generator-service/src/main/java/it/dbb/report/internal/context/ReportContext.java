package it.dbb.report.internal.context;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.Validator;

import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.core.document.DocumentKind;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;

public class ReportContext implements Closeable {

	private InputStream originalInputStream;
	private IXDocReport input;
	private IContext context;
	private Options options;
	private boolean report;
	private boolean convert;

	public ReportContext(InputStream inputStream, String mimeTypeIn, String mimeTypeOut, Map<String, Object> context)
			throws XDocReportException, IOException {

		originalInputStream = inputStream;
		
		// create context java model
		if (context != null && !context.isEmpty()) {

			// load the zipped xml file (odt, docx, etc...)
			input = getReport(inputStream);
			this.context = input.createContext(context);
			this.report = true;

		} else {

			this.report = false;
		}

		// set the format converter
		// search through known available conversion types from
		for(DocumentKind documentKind : DocumentKind.values()) {
			
			if(documentKind.getMimeType().equals(mimeTypeIn)) {
				
				options = Options.getFrom(documentKind);
			}
		}
		
		if(options == null) {
			
			options = Options.getFrom(mimeTypeIn);
		}

		if (Validator.isNotNull(mimeTypeOut) && !mimeTypeOut.equalsIgnoreCase(mimeTypeIn)) {

			// search through known available conversion types to
			for (ConverterTypeTo converterTypeTo : ConverterTypeTo.values()) {
				
				if(converterTypeTo.getMimeType().equals(mimeTypeOut)) {
					
					options.to(converterTypeTo);
				}
			}
			
			if(Validator.isNull(options.getTo())) {
				
				options.to(mimeTypeOut);
			}
			
			this.convert = true;

		} else {

			this.convert = false;
		}
	}
	
	public InputStream getOriginalInputStream() {
		
		return originalInputStream;
	}

	public IXDocReport getInput() {

		return input;
	}

	public IContext getContext() {

		return context;
	}

	public Options getOptions() {

		return options;
	}

	public boolean isReport() {

		return report;
	}

	public boolean isConvert() {

		return convert;
	}
	
	@Override
	public void close() throws IOException {
		
		StreamUtil.cleanUp(true, originalInputStream);
	}

	protected IXDocReport getReport(InputStream inputStream) throws XDocReportException, IOException {

		return XDocReportRegistry.getRegistry().loadReport(inputStream, TemplateEngineKind.Freemarker);
	}
}
