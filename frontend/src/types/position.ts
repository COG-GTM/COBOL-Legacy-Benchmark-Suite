/**
 * Position interface derived from POSMAP fields in src/maps/INQSET.bms
 * and src/copybook/common/POSREC.cpy
 */

export interface Position {
  fundId: string;
  fundName: string;
  units: number;
  costBasis: number;
  marketValue: number;
}
