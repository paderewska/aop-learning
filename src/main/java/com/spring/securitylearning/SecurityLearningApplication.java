package com.spring.securitylearning;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

@SpringBootApplication
public class SecurityLearningApplication {

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	public static void main(String[] args) {
		SpringApplication.run(SecurityLearningApplication.class, args);
	}

	@Component
	@Aspect
	class LoggingAspect {

		private final Log log = LogFactory.getLog(getClass());

		@Around("execution( * com.spring..*.*(..) )")
		public Object log(ProceedingJoinPoint pjp) throws Throwable {
			this.log.info("before " + pjp.toString());
			Object object = pjp.proceed();
			this.log.info("after " + pjp.toString());
			return object;
		}
	}

	@Component
	class LoggingFilter implements javax.servlet.Filter {

		private final Log log = LogFactory.getLog(getClass());

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void destroy() {
		}

		@Override
		public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

			Assert.isTrue(servletRequest instanceof HttpServletRequest, "this assumes you have an HTTP request");

			HttpServletRequest httpServletRequest = HttpServletRequest.class.cast(servletRequest);
			String uri = httpServletRequest.getRequestURI();
			this.log.info("new request for " + uri + ".");
			long time = System.currentTimeMillis();
			filterChain.doFilter(servletRequest, servletResponse);
			long delta = System.currentTimeMillis() - time;
			this.log.info("request for " + uri + " took " + delta + "ms");

		}
	}

	@RestController
	class IsbnRestController {

		private final RestTemplate restTemplate;

		public IsbnRestController(RestTemplate restTemplate) {
			this.restTemplate = restTemplate;
		}

		@GetMapping("/books/{isbn}")
		String lookUpBookByIsbn(@PathVariable("isbn") String isbn) {
			ResponseEntity<String> exchange = this.restTemplate.exchange("https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn,
					HttpMethod.GET, null, String.class);
			return exchange.getBody();
		}
	}

	@Component("uuid")
	class UuidService {

		public String buildUuid() {
			return UUID.randomUUID().toString();
		}
	}

	class LoggingUuidService extends UuidService {

		@Override
		public String buildUuid() {
			LogFactory.getLog(getClass()).info("before...");
			String result = super.buildUuid();
			LogFactory.getLog(getClass()).info("after...");
			return result;
		}
	}
}
