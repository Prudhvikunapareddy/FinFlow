export type DocumentType = 'SALARY_SLIP' | 'BANK_STATEMENT' | 'ID_PROOF' | 'ADDRESS_PROOF' | 'OTHER';

export interface DocumentResponse {
  id: number;
  fileName: string;
  fileType: string;
  documentType?: DocumentType;
  applicationId: number;
}
