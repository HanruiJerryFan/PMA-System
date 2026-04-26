export function normalizeAuthorities(user) {
  if (!user || !Array.isArray(user.authorities)) {
    return [];
  }
  return user.authorities.filter(Boolean);
}

export function hasAnyAuthority(user, authorities = []) {
  if (!Array.isArray(authorities) || authorities.length === 0) {
    return true;
  }
  const currentAuthorities = normalizeAuthorities(user);
  return authorities.some((authority) => currentAuthorities.includes(authority));
}
