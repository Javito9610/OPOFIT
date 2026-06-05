/**
 * Utilidades para mocks de Express req/res en tests unitarios de controladores.
 */
function mockRes() {
  const res = {};
  res.status = jest.fn().mockReturnValue(res);
  res.json = jest.fn().mockReturnValue(res);
  res.send = jest.fn().mockReturnValue(res);
  res.redirect = jest.fn().mockReturnValue(res);
  return res;
}

function mockReq({ body = {}, params = {}, query = {}, usuario = null, headers = {} } = {}) {
  const req = { body, params, query, usuario, headers };
  req.header = (name) => headers[name] || headers[name.toLowerCase()];
  return req;
}

module.exports = { mockReq, mockRes };
