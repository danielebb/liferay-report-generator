package it.dbb.report.internal.context;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.Validator;

import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.core.document.DocumentKind;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.images.ByteArrayImageProvider;
import fr.opensagres.xdocreport.document.images.FileImageProvider;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldMetadata;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;

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

			injectImages(context);

		} else {

			this.report = false;
		}

		// set the format converter
		// search through known available conversion types from
		for (DocumentKind documentKind : DocumentKind.values()) {

			if (documentKind.getMimeType().equals(mimeTypeIn)) {

				options = Options.getFrom(documentKind);
			}
		}

		if (options == null) {

			options = Options.getFrom(mimeTypeIn);
		}

		if (Validator.isNotNull(mimeTypeOut) && !mimeTypeOut.equalsIgnoreCase(mimeTypeIn)) {

			// search through known available conversion types to
			for (ConverterTypeTo converterTypeTo : ConverterTypeTo.values()) {

				if (converterTypeTo.getMimeType().equals(mimeTypeOut)) {

					options.to(converterTypeTo);
				}
			}

			if (Validator.isNull(options.getTo())) {

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

	protected void injectImages(Map<String, Object> context) {

		context.replaceAll((key, value) -> {

			if (value instanceof InputStream) {

				_log.info("Replacing object " + key + " with a " + ByteArrayImageProvider.class.getName());

				try {

					FieldsMetadata metadata = getInputFieldsMetadata();
					metadata.addFieldAsImage(key);

					return new ByteArrayImageProvider((InputStream) value);

				} catch (IOException e) {

					_log.error(e);
				}

			} else if (value instanceof File) {

				_log.info("Replacing object " + key + " with a " + FileImageProvider.class.getName());

				FieldsMetadata metadata = getInputFieldsMetadata();
				metadata.addFieldAsImage(key);

				return new FileImageProvider((File) value);
			}

			return value;
		});
	}

	protected FieldsMetadata getInputFieldsMetadata() {

		FieldsMetadata fieldsMetadata = input.getFieldsMetadata();

		if(fieldsMetadata == null) {

			fieldsMetadata = new FieldsMetadata();
			input.setFieldsMetadata(fieldsMetadata);
		}

		return fieldsMetadata;
	}

	private static final Log _log = LogFactoryUtil.getLog(ReportContext.class);
}
