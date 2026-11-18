package com.mfaraujo.financialapi.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.mfaraujo.financialapi.config.ApiAccessProperties;
import com.mfaraujo.financialapi.domain.model.BillDetail;
import com.mfaraujo.financialapi.domain.result.BillDetailResult;
import com.mfaraujo.financialapi.exception.GlobalExceptionHandler;
import com.mfaraujo.financialapi.exception.PluggyException;
import com.mfaraujo.financialapi.resource.FinancialDataController;
import com.mfaraujo.financialapi.service.AccountService;
import com.mfaraujo.financialapi.service.BillService;
import com.mfaraujo.financialapi.service.LoanService;
import com.mfaraujo.financialapi.service.SummaryService;
import com.mfaraujo.financialapi.service.TransactionListService;
import com.mfaraujo.financialapi.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** {@code GET /bill/{id}} com o controller real, o handler de erros e os filtros de CORS e de chave. */
class BillDetailRouteTest {

	private static final String KEY = "chave-de-teste";

	private static final String FRONT = "https://front.exemplo.com";

	private BillService billService;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		billService = mock(BillService.class);
		FinancialDataController controller = new FinancialDataController(mock(AccountService.class),
				mock(TransactionService.class), billService, mock(SummaryService.class), mock(LoanService.class),
				mock(TransactionListService.class));
		ApiAccessProperties properties = new ApiAccessProperties(KEY, new ApiAccessProperties.Cors(List.of(FRONT)));
		mvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new GlobalExceptionHandler())
				.addFilters(AccessControlConfig.corsFilter(properties),
						new ApiKeyFilter(properties, JsonMapper.builder().build()))
				.build();
	}

	@Test
	void withoutTheKeyTheRouteIsUnauthorized() throws Exception {
		mvc.perform(get("/bill/x").param("owner", "marcelo").param("accountId", "c1"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
		verifyNoInteractions(billService);
	}

	@Test
	void withAWrongKeyTheRouteIsUnauthorized() throws Exception {
		mvc.perform(get("/bill/x").header("X-API-Key", "errada").param("owner", "marcelo").param("accountId", "c1"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void thePreflightOfAnAllowedOriginPassesWithoutTheKey() throws Exception {
		mvc.perform(options("/bill/x").header("Origin", FRONT).header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "x-api-key")).andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", FRONT));
	}

	@Test
	void aMissingOwnerIsABadRequestInTheStandardFormat() throws Exception {
		mvc.perform(get("/bill/x").header("X-API-Key", KEY).param("accountId", "c1"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").exists()).andExpect(jsonPath("$.message").exists());
		verifyNoInteractions(billService);
	}

	@Test
	void aMissingAccountIdIsABadRequestInTheStandardFormat() throws Exception {
		mvc.perform(get("/bill/x").header("X-API-Key", KEY).param("owner", "marcelo"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").exists()).andExpect(jsonPath("$.message").exists());
	}

	@Test
	void aBlankOwnerOrAccountIdIsABadRequest() throws Exception {
		mvc.perform(get("/bill/x").header("X-API-Key", KEY).param("owner", "").param("accountId", "c1"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
		mvc.perform(get("/bill/x").header("X-API-Key", KEY).param("owner", "marcelo").param("accountId", " "))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
		verifyNoInteractions(billService);
	}

	@Test
	void aValidRequestReturnsTheDetail() throws Exception {
		BillDetail bill = new BillDetail("x", LocalDate.of(2026, 11, 20), null, new BigDecimal("10"), "BRL", null,
				null, List.of(), List.of());
		when(billService.getBillDetail("marcelo", "x", "c1")).thenReturn(new BillDetailResult(bill, List.of()));

		mvc.perform(get("/bill/x").header("X-API-Key", KEY).param("owner", "marcelo").param("accountId", "c1"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.owner").value("marcelo"))
				.andExpect(jsonPath("$.accountId").value("c1")).andExpect(jsonPath("$.bill.id").value("x"))
				.andExpect(jsonPath("$.bill.dueDate").value("2026-11-20"))
				.andExpect(jsonPath("$.transactions").isEmpty());
		verify(billService).getBillDetail("marcelo", "x", "c1");
	}

	@Test
	void aMissingBillInPluggyIsNotFound() throws Exception {
		when(billService.getBillDetail(any(), any(), any()))
				.thenThrow(new PluggyException("marcelo", HttpStatus.NOT_FOUND, "Fatura não encontrada"));

		mvc.perform(get("/bill/x").header("X-API-Key", KEY).param("owner", "marcelo").param("accountId", "c1"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
	}

}
