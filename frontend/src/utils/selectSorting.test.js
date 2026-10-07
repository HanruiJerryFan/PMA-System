import { sortSelectItems } from "./selectSorting";

test("numbered business options use natural order and put missing numbers last", () => {
  const rows = [
    { uuid: "ten", projectNumber: "P10" },
    { uuid: "missing" },
    { uuid: "two", projectNumber: "P2" },
    { uuid: "one", projectNumber: " P1 " },
    { uuid: "blank", projectNumber: " " },
  ];
  expect(sortSelectItems(rows, ["projectNumber"]).map((item) => item.uuid))
    .toEqual(["one", "two", "ten", "missing", "blank"]);
});

test("dictionary ordering takes priority over codes and codes break ties", () => {
  const rows = [
    { code: "2", sortOrder: 2 },
    { code: "10", sortOrder: 1 },
    { code: "1", sortOrder: 2 },
  ];
  expect(sortSelectItems(rows, ["sortOrder", "code"]).map((item) => item.code))
    .toEqual(["10", "1", "2"]);
});

test("decimal numeric values and zero are compared numerically", () => {
  const rows = [{ rate: 0.9 }, { rate: 0 }, { rate: null }, { rate: 0.13 }, { rate: 0.09 }];
  expect(sortSelectItems(rows, ["rate"]).map((item) => item.rate))
    .toEqual([0, 0.09, 0.13, 0.9, null]);
});

test("sorting preserves source order, objects, and selected IDs", () => {
  const ten = Object.freeze({ uuid: "ten", materialCode: "M10" });
  const two = Object.freeze({ uuid: "two", materialCode: "M2" });
  const rows = Object.freeze([ten, two]);
  const sorted = sortSelectItems(rows, ["materialCode"]);
  expect(rows).toEqual([ten, two]);
  expect(sorted).toEqual([two, ten]);
  expect(sorted[0]).toBe(two);
});

test("code strings and model numbers sort without losing leading zeros", () => {
  expect(sortSelectItems(["A10", "", "A02", "A1"])).toEqual(["A1", "A02", "A10", ""]);
});

test("derived business numbers can be sorted with a selector", () => {
  const rows = [{ detail: { voucherNo: "2026100610" } }, { detail: { voucherNo: "2026100602" } }];
  expect(sortSelectItems(rows, [(item) => item.detail.voucherNo])).toEqual([rows[1], rows[0]]);
});
