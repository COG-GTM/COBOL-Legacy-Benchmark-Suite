package com.clbs.portfolio.web;

import com.clbs.portfolio.batch.BatchReport;
import com.clbs.portfolio.batch.PortfolioBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/batch/portfolios")
@Tag(name = "Batch", description = "Sequential-file batch jobs using the original fixed-width record layouts")
public class BatchController {

    private final PortfolioBatchService batchService;

    public BatchController(PortfolioBatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping(value = "/add", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(operationId = "batchAdd", summary = "PORTADD: 148-byte PORTFLIO records (COMP-3 amounts)")
    public BatchReport add(@RequestBody byte[] file) {
        return batchService.add(file);
    }

    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(operationId = "batchUpdate", summary = "PORTUPDT: 69-byte update records")
    public BatchReport update(@RequestBody byte[] file) {
        return batchService.update(file);
    }

    @PostMapping(value = "/delete", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(operationId = "batchDelete", summary = "PORTDEL: 80-byte delete records")
    public BatchReport delete(@RequestBody byte[] file) {
        return batchService.delete(file);
    }

    @GetMapping("/report")
    @Operation(operationId = "batchReport", summary = "PORTREAD: listing of all portfolios")
    public BatchReport report() {
        return batchService.report();
    }
}
