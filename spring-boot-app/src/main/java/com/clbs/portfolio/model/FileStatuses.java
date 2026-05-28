package com.clbs.portfolio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embedded file status tracking for checkpoint/restart.
 * From COBOL copybook: src/copybook/batch/CKPRST.cpy (CK-FILE-STATUS OCCURS 5 TIMES).
 * Flattened to 5 slots matching the COBOL OCCURS clause.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStatuses {

    @Column(name = "file1_name", length = 8)
    private String file1Name;
    @Column(name = "file1_pos", length = 50)
    private String file1Pos;
    @Column(name = "file1_status", length = 2)
    private String file1Status;

    @Column(name = "file2_name", length = 8)
    private String file2Name;
    @Column(name = "file2_pos", length = 50)
    private String file2Pos;
    @Column(name = "file2_status", length = 2)
    private String file2Status;

    @Column(name = "file3_name", length = 8)
    private String file3Name;
    @Column(name = "file3_pos", length = 50)
    private String file3Pos;
    @Column(name = "file3_status", length = 2)
    private String file3Status;

    @Column(name = "file4_name", length = 8)
    private String file4Name;
    @Column(name = "file4_pos", length = 50)
    private String file4Pos;
    @Column(name = "file4_status", length = 2)
    private String file4Status;

    @Column(name = "file5_name", length = 8)
    private String file5Name;
    @Column(name = "file5_pos", length = 50)
    private String file5Pos;
    @Column(name = "file5_status", length = 2)
    private String file5Status;
}
