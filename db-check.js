async function checkIvolveDBExists(connection) {
  const [rows] = await connection.query("SHOW DATABASES LIKE 'ivolve'");
  return rows.length > 0;
}

module.exports = checkIvolveDBExists;
