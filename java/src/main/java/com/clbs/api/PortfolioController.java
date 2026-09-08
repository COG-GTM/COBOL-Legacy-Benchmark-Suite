package com.clbs.api;

import com.clbs.common.ProgramResult;
import com.clbs.domain.PortfolioRecord;
import com.clbs.portfolio.PortfolioAddProgram;
import com.clbs.portfolio.PortfolioDeleteProgram;
import com.clbs.portfolio.PortfolioMasterService;
import com.clbs.portfolio.PortfolioReadProgram;
import com.clbs.portfolio.PortfolioUpdateProgram;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry points for the PORTMSTR/PORTADD/PORTUPDT/PORTDEL/PORTREAD programs. */
@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioMasterService master;
    private final PortfolioAddProgram addProgram;
    private final PortfolioUpdateProgram updateProgram;
    private final PortfolioDeleteProgram deleteProgram;
    private final PortfolioReadProgram readProgram;

    public PortfolioController(PortfolioMasterService master, PortfolioAddProgram addProgram,
            PortfolioUpdateProgram updateProgram, PortfolioDeleteProgram deleteProgram,
            PortfolioReadProgram readProgram) {
        this.master = master;
        this.addProgram = addProgram;
        this.updateProgram = updateProgram;
        this.deleteProgram = deleteProgram;
        this.readProgram = readProgram;
    }

    /** PORTREAD. */
    @GetMapping
    public ProgramResult list() {
        return readProgram.run();
    }

    /** PORTMSTR command 'R'. */
    @GetMapping("/{portId}")
    public ResponseEntity<PortfolioMasterService.Response> read(@PathVariable String portId,
            @RequestParam(defaultValue = "") String accountNo) {
        PortfolioMasterService.Response response = master.read(portId, accountNo);
        return response.ok() ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /** PORTMSTR command 'C'. */
    @PostMapping
    public ResponseEntity<PortfolioMasterService.Response> create(
            @RequestBody PortfolioRecord record) {
        PortfolioMasterService.Response response = master.create(record);
        return response.ok() ? ResponseEntity.status(HttpStatus.CREATED).body(response)
                : ResponseEntity.badRequest().body(response);
    }

    /** PORTMSTR command 'U'. */
    @PutMapping("/{portId}")
    public ResponseEntity<PortfolioMasterService.Response> update(@PathVariable String portId,
            @RequestBody PortfolioRecord record) {
        record.setPortId(portId);
        PortfolioMasterService.Response response = master.update(record);
        return response.ok() ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /** PORTMSTR command 'D'. */
    @DeleteMapping("/{portId}")
    public ResponseEntity<PortfolioMasterService.Response> delete(@PathVariable String portId,
            @RequestParam(defaultValue = "") String accountNo) {
        PortfolioMasterService.Response response = master.delete(portId, accountNo);
        return response.ok() ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /** PORTADD batch load. */
    @PostMapping("/batch-add")
    public ProgramResult batchAdd(@RequestBody List<PortfolioRecord> records) {
        return addProgram.run(records);
    }

    /** PORTUPDT batch update. */
    @PostMapping("/batch-update")
    public ProgramResult batchUpdate(
            @RequestBody List<PortfolioUpdateProgram.UpdateRequest> requests) {
        return updateProgram.run(requests);
    }

    /** PORTDEL batch delete. */
    @PostMapping("/batch-delete")
    public ProgramResult batchDelete(
            @RequestBody List<PortfolioDeleteProgram.DeleteRequest> requests) {
        return deleteProgram.run(requests);
    }
}
