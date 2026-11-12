package cl.elvolcan.ventas.exception;

import cl.elvolcan.common.exception.ApiErrors;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;

/** @ControllerAdvice aplica los @ExceptionHandler heredados de common a todos los controladores. */
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler extends ApiErrors {}
