export type Tenant = { tenantId: string; membershipId: string; name: string; slug: string; membershipType: string; membershipStatus: string };
export type Account = { id: string; email: string; displayName: string; status: string; platformAdmin: boolean; mustChangePassword: boolean };
export type TokenPair = { accessToken: string; refreshToken: string };
export type LoginResult = { userId: string; tenantId: string; membershipId: string; tokens: TokenPair };
export type FormField = {
  id: string;
  type: 'text' | 'textarea' | 'number' | 'date' | 'time' | 'select' | 'multiselect' | 'checkbox' | 'photo' | 'signature' | 'heading' | 'instructions';
  label: string; description?: string; required?: boolean; options?: string[];
};
export type FormSection = { id: string; title: string; description?: string; fields: FormField[] };
export type FormDefinition = { sections: FormSection[] };
export type PublishedForm = { formId: string; tenantId: string; name: string; description?: string; versionId: string; version: number; definition: FormDefinition; publishedAt?: string };
export type PullResult = { cursor: string; hasMore: boolean; forms: PublishedForm[]; tombstones: { resourceType: string; resourceId: string }[] };
export type MutationResult = { mutationId: string; status: 'APPLIED' | 'ALREADY_APPLIED' | 'CONFLICT' | 'REJECTED'; message?: string };
