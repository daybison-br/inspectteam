import assert from 'node:assert/strict';
import test from 'node:test';
import { hasValue, requiredErrors } from '../src/features/form/validation';

test('considera respostas válidas sem aceitar valores vazios', () => {
  assert.equal(hasValue('vistoriado'), true);
  assert.equal(hasValue(0), true);
  assert.equal(hasValue(['sim']), true);
  assert.equal(hasValue(''), false);
  assert.equal(hasValue([]), false);
  assert.equal(hasValue(false), false);
});

test('retorna somente campos obrigatórios não respondidos', () => {
  const fields = [
    { id: 'placa', type: 'text' as const, label: 'Placa', required: true },
    { id: 'observacao', type: 'textarea' as const, label: 'Observação' },
    { id: 'ok', type: 'checkbox' as const, label: 'Conferido', required: true },
  ];
  assert.deepEqual(requiredErrors(fields, { placa: 'ABC1D23', ok: false }), { ok: 'Este campo é obrigatório.' });
});
