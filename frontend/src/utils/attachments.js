import { attachmentAPI } from "../api/modules";

export function normalizeAttachmentResponse(response) {
  return response?.data ?? response ?? null;
}

export function normalizeSelectedFiles(value) {
  if (!value) {
    return [];
  }
  if (Array.isArray(value)) {
    return value;
  }
  return [value];
}

export async function uploadBusinessAttachments(businessType, businessUuid, files) {
  const selectedFiles = normalizeSelectedFiles(files);
  const uploadedAttachments = [];

  for (const file of selectedFiles) {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("businessType", businessType);
    formData.append("businessUuid", businessUuid);
    const response = await attachmentAPI.uploadAttachment(formData);
    uploadedAttachments.push(normalizeAttachmentResponse(response));
  }

  return uploadedAttachments.filter(Boolean);
}
