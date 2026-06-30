export interface Account {
  id: string;
  accountNumber: string;
  ownerName: string;
  currency: string;
  status: string;
  balanceMinor: number;
  balance: number;
}

export interface Posting {
  id: string;
  accountId: string;
  amountMinor: number;
  currency: string;
}

export interface Transaction {
  id: string;
  reference: string;
  description: string | null;
  createdAt: string;
  postings: Posting[];
}

export interface UserInfo {
  username: string;
  role: string;
}

export interface Beneficiary {
  id: string;
  label: string;
  accountNumber: string;
}

export interface AuditEntry {
  id: string;
  at: string;
  actor: string | null;
  action: string;
  detail: string | null;
}

export interface TransactionLine {
  id: string;
  at: string;
  amountMinor: number;
  currency: string;
  description: string | null;
  kind: 'DEPOSIT' | 'TRANSFER';
  counterpartyName: string | null;
  counterpartyNumber: string | null;
  category: string | null;
  balanceAfterMinor: number;
}

export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  body: string | null;
  read: boolean;
  at: string;
}

export interface PaymentRequest {
  id: string;
  requesterName: string;
  payerName: string;
  toAccountNumber: string | null;
  amountMinor: number;
  currency: string;
  description: string | null;
  status: 'PENDING' | 'ACCEPTED' | 'REFUSED' | 'CANCELLED';
  createdAt: string;
  resolvedAt: string | null;
}

export interface AuditPage {
  items: AuditEntry[];
  page: number;
  size: number;
  total: number;
  hasMore: boolean;
}
