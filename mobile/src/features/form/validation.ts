import type { FormField } from '@/core/api/contracts';

export function hasValue(value: unknown) {
  return value !== undefined && value !== null && value !== '' && (!Array.isArray(value) || value.length > 0) && value !== false;
}

export function requiredErrors(fields: FormField[], answers: Record<string, unknown>) {
  return Object.fromEntries(fields.filter(field => field.required && !hasValue(answers[field.id])).map(field => [field.id, 'Este campo é obrigatório.']));
}
