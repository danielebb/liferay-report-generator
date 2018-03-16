package it.dbb.report.api.exceptions;

import com.liferay.portal.kernel.exception.PortalException;

public class ConversionException extends PortalException {

	public ConversionException() {
		super();
	}

	public ConversionException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public ConversionException(String msg) {
		super(msg);
	}

	public ConversionException(Throwable cause) {
		super(cause);
	}

}
