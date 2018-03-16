package it.dbb.report.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.liferay.document.library.kernel.model.DLFileEntry;
import com.liferay.document.library.kernel.service.DLFileEntryLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import fr.opensagres.xdocreport.converter.ConverterFrom;
import fr.opensagres.xdocreport.converter.ConverterRegistry;
import fr.opensagres.xdocreport.converter.ConverterTo;
import fr.opensagres.xdocreport.converter.IConverter;
import fr.opensagres.xdocreport.converter.XDocConverterException;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import it.dbb.report.api.ReportGenerator;
import it.dbb.report.api.exceptions.ConversionException;
import it.dbb.report.api.exceptions.ReportException;
import it.dbb.report.internal.context.ReportContext;

/**
 * @author daniele
 */
@Component(service = ReportGenerator.class)
public class XdocReportGeneratorImpl implements ReportGenerator {

	@Override
	public void generateReport(InputStream template, OutputStream output, String inputMimeType, String outputMimeType,
			Map<String, Object> context) throws PortalException, IOException {

		try (ReportContext reportContext = new ReportContext(template, inputMimeType, outputMimeType, context)) {

			xdocReport(output, reportContext);

		} catch (XDocReportException e) {

			throw new PortalException(e);
		}
	}

	@Override
	public void generateReport(long fileEntryId, OutputStream output, String outputMimeType,
			Map<String, Object> context) throws PortalException, IOException {

		DLFileEntry fileEntry = DLFileEntryLocalServiceUtil.getDLFileEntry(fileEntryId);

		try (ReportContext reportContext = new ReportContext(fileEntry.getContentStream(), fileEntry.getMimeType(),
				outputMimeType, context)) {

			xdocReport(output, reportContext);

		} catch (XDocReportException e) {

			throw new PortalException(e);
		}
	}

	protected void xdocReport(OutputStream output, ReportContext reportContext)
			throws ConversionException, ReportException, PortalException, IOException {

		IXDocReport input = reportContext.getInput();

		if (reportContext.isReport()) {

			try {

				if (reportContext.isConvert()) {

					// report AND convert

					input.convert(reportContext.getContext(), reportContext.getOptions(), output);

				} else {

					// just report

					input.process(reportContext.getContext(), output);
				}

			} catch (XDocConverterException xdce) {

				throw new ConversionException(xdce);

			} catch (XDocReportException xdre) {

				throw new ReportException(xdre);
			}

		} else if (reportContext.isConvert()) {

			// just convert

			IConverter converter = ConverterRegistry.getRegistry().getConverter(reportContext.getOptions());

			if (converter != null) {

				try {

					converter.convert(reportContext.getOriginalInputStream(), output, reportContext.getOptions());

				} catch (XDocConverterException xdce) {

					throw new ConversionException(xdce);
				}

			} else {

				throw new ConversionException("No suitable converter from " + reportContext.getOptions().getFrom()
						+ " to " + reportContext.getOptions().getTo() + " was found");
			}

		} else {

			throw new PortalException("No operation could be done with these parameters");
		}
	}

	@Activate
	public void activate() {

		ConverterRegistry converterRegistry = ConverterRegistry.getRegistry();

		if (converterRegistry.getFroms().isEmpty()) {

			_log.warn("Registry is empty, reinitializing");

			// reinitialize the registry
			converterRegistry.dispose();
			converterRegistry.initialize();
		}

		/**
		 * Used for debug only
		 */
		// if (converterRegistry.getFroms().isEmpty()) {
		//
		// _log.warn("Registry is empty, force initialization of known converters");
		//
		// try {
		//
		// // force register the converters
		// Method method =
		// converterRegistry.getClass().getDeclaredMethod("registerInstance",
		// IConverterDiscovery.class);
		// if (!method.isAccessible()) {
		//
		// method.setAccessible(true);
		// }
		//
		// ODF2PDFViaITextConverterDiscovery odf2pdfViaITextConverterDiscovery = new
		// ODF2PDFViaITextConverterDiscovery();
		// IConverter iConverter = odf2pdfViaITextConverterDiscovery.getConverter();
		// method.invoke(converterRegistry, odf2pdfViaITextConverterDiscovery);
		//
		// } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
		// IllegalArgumentException
		// | InvocationTargetException e) {
		//
		// _log.error("Error initializing the register.", e);
		// }
		// }

		if (_log.isInfoEnabled()) {

			_log.info("Available converters:");

			for (String from : converterRegistry.getFroms()) {

				ConverterFrom converterFrom = converterRegistry.getConverterFrom(from);

				for (ConverterTo converterTo : converterFrom.getConvertersTo()) {

					_log.info("From " + converterFrom.getFrom() + " to " + converterTo.getTo());
				}
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(XdocReportGeneratorImpl.class);
}